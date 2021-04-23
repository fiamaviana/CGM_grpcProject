package GUI;

import java.awt.*;

import javax.swing.*;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;


import com.proto.mobileApp.AppRequest;
import com.proto.mobileApp.AppResponse;
import com.proto.mobileApp.MobileApp;
import com.proto.mobileApp.MobileServiceGrpc;
import com.proto.transmitter.Transmitter;
import com.proto.transmitter.TransmitterRequest;
import com.proto.transmitter.TransmitterResponse;
import com.proto.transmitter.TransmitterServiceGrpc;
import com.proto.transmitter.TransmitterServiceGrpc.TransmitterServiceBlockingStub;
import com.proto.transmitter.TransmitterServiceGrpc.TransmitterServiceStub;
import com.proto.watchApp.WatchApp;
import com.proto.watchApp.AverageRequest;
import com.proto.watchApp.WatchServiceGrpc;
import com.proto.watchApp.WatchServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;


import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

public class GUI {
    private static TransmitterServiceBlockingStub TransmitterBlockingStub;
    private static TransmitterServiceStub TransmitterAsyncStub;
    private static MobileServiceGrpc.MobileServiceStub mobileServiceStub;
    private static WatchServiceGrpc.WatchServiceBlockingStub watchServiceBlockingStub;


    private ServiceInfo serviceInfo;

    private JFrame frame;
    private JTextField textNumber1;
    private JTextArea textResponse ;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                GUI window = new GUI();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Create the application.
     */
    public GUI(){

        //discovering transmitter server by jmdns
        String transmitter_service = "_transmitter._tcp.local.";
        discoverService(transmitter_service);

        String host = serviceInfo.getHostAddresses()[0];
        int port = serviceInfo.getPort();

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        //stubs -- generate from proto
        TransmitterBlockingStub = TransmitterServiceGrpc.newBlockingStub(channel);
        TransmitterAsyncStub = TransmitterServiceGrpc.newStub(channel);


        //discovering mobile app server by jmdns
        String mobile_service = "_app._tcp.local.";
        discoverService(mobile_service);

        String host2 = serviceInfo.getHostAddresses()[0];
        int port2 = serviceInfo.getPort();

        ManagedChannel channel2 = ManagedChannelBuilder.forAddress(host2, port2)
                .usePlaintext()
                .build();

        mobileServiceStub = MobileServiceGrpc.newStub(channel2);


        //discovering watch app server by jmdns
        String watch_service = "_watch._tcp.local.";
        discoverService(watch_service);

        String host3 = serviceInfo.getHostAddresses()[0];
        int port3 = serviceInfo.getPort();

        ManagedChannel channel1 = ManagedChannelBuilder.forAddress(host3, port3)
                .usePlaintext()
                .build();
        watchServiceBlockingStub = WatchServiceGrpc.newBlockingStub(channel1);

        initialize();

    }

    private void discoverService(String service_type) {
        try {
            String address = InetAddress.getLocalHost().toString().split("/")[1];
            JmDNS jmdns = JmDNS.create(address);
            System.out.println("Discovering service...");


            jmdns.addServiceListener(service_type, new ServiceListener(){

                @Override
                public void serviceAdded(ServiceEvent event) {
                    System.out.println("Transmitter Service added: " + event.getInfo());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Transmitter Service removed: " + event.getInfo());

                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    System.out.println("Transmitter Service resolved: " + event.getInfo());
                    serviceInfo = event.getInfo();
                    int port = serviceInfo.getPort();


                    System.out.println("resolving " + service_type + " with properties ...");
                    System.out.println("\t port: " + port);
                    System.out.println("\t type:"+ event.getType());
                    System.out.println("\t name: " + event.getName());
                    System.out.println("\t description/properties: " + serviceInfo.getNiceTextString());
                    System.out.println("\t host: " + serviceInfo.getHostAddresses()[0]);
                }
            });
            // Wait a bit
            Thread.sleep(1000);

            jmdns.close();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Transmitter Service");
        frame.setBounds(500, 500, 500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BoxLayout bl = new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS);

        frame.getContentPane().setLayout(bl);

        JPanel panel_service_1 = new JPanel();
        frame.getContentPane().add(panel_service_1);
        panel_service_1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel lblNewLabel_1 = new JLabel("Blood Glucose Level: ");
        panel_service_1.add(lblNewLabel_1);

        textNumber1 = new JTextField();
        panel_service_1.add(textNumber1);
        textNumber1.setColumns(10);


        ArrayList<Integer> glucoseArray = new ArrayList<>();

        JButton btn = new JButton("Add Glucose Level");
        btn.addActionListener(e -> {

            try{

                double number = Double.parseDouble(textNumber1.getText());
                glucoseArray.add((int) number);

                //created a protocol buffer transmitter message
                Transmitter transmitter = Transmitter.newBuilder()
                        .setGlucoseLevel(number)
                        .build();
                //do the same for a transmitter Request
                TransmitterRequest request = TransmitterRequest.newBuilder()
                        .setRequest(transmitter)
                        .build();

                TransmitterResponse response = TransmitterBlockingStub.transmitter(request);

                textResponse.append("Blood Sugar Level: " + response.getResult() + " mg/dL" + "\n");

                System.out.println("Blood Sugar Level: " + response.getResult() + " mg/dL");


            }catch (Exception exception){
                JOptionPane.showMessageDialog(frame,"Please check the input entered.");
                //exception.printStackTrace();
            }

        });
        JButton btn2 = new JButton("Highest Glucose Level");
        btn2.addActionListener(e -> {
            StreamObserver<AppRequest> requestObserver = mobileServiceStub.app(new StreamObserver<AppResponse>() {
                @Override
                public void onNext(AppResponse value) {
                    System.out.println("The highest blood level is: " + value.getResult() + " mg/dL");
                    JOptionPane.showMessageDialog(frame,"The highest blood sugar level is: " + value.getResult() + " mg/dL");
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Highest blood sugar service got an error.");
                }

                @Override
                public void onCompleted() {
                    //doesn't need do anything
                }
            });
            //sending the glucose Array data to server
            for (int j : glucoseArray) {
                requestObserver.onNext(AppRequest.newBuilder()
                        .setRequest(MobileApp.newBuilder()
                                .setGlucoseLevel(j)).build());
            }
            //we tell the server that client is done
            requestObserver.onCompleted();

        });

        JButton btn3 = new JButton("Analysis");
        btn3.addActionListener(e -> {
            int count = 0;
            int sum = 0;
            try{
                for(int i=0; i < glucoseArray.size() ; i++){
                    count++;
                    sum += glucoseArray.get(i);
                }

                int average = sum/count;
                System.out.println("Your blood sugar average is: " + average + " mg/dL");
                textResponse.append("Your blood sugar average is: " + average + " mg/dL");

                watchServiceBlockingStub.app(AverageRequest.newBuilder().setRequest(WatchApp.newBuilder().setGlucoseLevel(average).build()).build())
                        .forEachRemaining(analysisResponse -> textResponse.append( "\n"+ analysisResponse.getResult()));

            }catch (Exception exception){
                exception.printStackTrace();
            }

        });

        panel_service_1.add(btn);

        textResponse = new JTextArea(10, 30);
        textResponse .setLineWrap(true);
        textResponse.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textResponse);

        //textResponse.setSize(new Dimension(30, 30));
        panel_service_1.add(scrollPane);

        panel_service_1.add(btn2);
        panel_service_1.add(btn3);

        JPanel panel_service_2 = new JPanel();
        frame.getContentPane().add(panel_service_2);

        JPanel panel_service_3 = new JPanel();
        frame.getContentPane().add(panel_service_3);


    }

}

