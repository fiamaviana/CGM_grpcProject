package CGM_Client;

import com.proto.mobileApp.AppRequest;
import com.proto.mobileApp.AppResponse;
import com.proto.mobileApp.MobileApp;
import com.proto.mobileApp.MobileServiceGrpc;
import com.proto.transmitter.*;
import com.proto.watchApp.AverageRequest;
import com.proto.watchApp.WatchApp;
import com.proto.watchApp.WatchServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CGMClient {
    private ServiceInfo serviceInfo;

    public int[] glucoseArray = new int[10];

    public void setGlucoseArray(int[]glucoseArray){
        this.glucoseArray = glucoseArray;
    }

    public int[] getGlucoseArray() {
        return glucoseArray;
    }

    public static void main(String[] args) {
        System.out.println("gRPC client is running");
        CGMClient main = new CGMClient();
        main.run();
    }

    private void run(){
        //discovering transmitter server by jmdns
        String transmitter_service = "_transmitter._tcp.local.";
        discoverService(transmitter_service);

        String host = serviceInfo.getHostAddresses()[0];
        int port = serviceInfo.getPort();

        //discovering mobile app server by jmdns
        String mobile_service = "_app._tcp.local.";
        discoverService(mobile_service);

        String host2 = serviceInfo.getHostAddresses()[0];
        int port2 = serviceInfo.getPort();

        //discovering watch app server by jmdns
        String watch_service = "_watch._tcp.local.";
        discoverService(watch_service);

        String host3 = serviceInfo.getHostAddresses()[0];
        int port3 = serviceInfo.getPort();

        ManagedChannel channel1 = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        UnaryCall(channel1);
        BidiStreamingCall(channel1);
        System.out.println();

        ManagedChannel channel2 = ManagedChannelBuilder.forAddress(host2, port2)
                .usePlaintext()
                .build();
        ClientStreamingCall(channel2);
        System.out.println();

        ManagedChannel channel3 = ManagedChannelBuilder.forAddress(host3, port3)
                .usePlaintext()
                .build();

        ServerStreamingCall(channel3);
        System.out.println();
        System.out.println("Shutting down channels");
        channel1.shutdown();
        channel2.shutdown();
        channel3.shutdown();

    }

    public void discoverService(String service_type) {
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

    private void UnaryCall(ManagedChannel channel) {
        //created a transmitter service client (blocking - synchronous)
        TransmitterServiceGrpc.TransmitterServiceBlockingStub transmitterClient = TransmitterServiceGrpc.newBlockingStub(channel);

        double number = 7.8; // the input is in mmol/L and will be converted to mg/dL

        try{
            //created a protocol buffer transmitter message
            Transmitter transmitter = Transmitter.newBuilder()
                    .setGlucoseLevel(number)
                    .build();
            //do the same for a transmitter Request
            TransmitterRequest request = TransmitterRequest.newBuilder()
                    .setRequest(transmitter)
                    .build();
            //call the RPC and get back a Response (protocol buffers)

            TransmitterResponse response = transmitterClient.transmitter(request);

            System.out.println("Glucose level: " + response.getResult());
            System.out.println("Unary Streaming successfully completed.");
            System.out.println();
        }catch (RuntimeException e){
            System.out.println("Handling exception...");
            e.printStackTrace();
        }

    }


    private void BidiStreamingCall(ManagedChannel channel) {
        //create an asynchronous client
        TransmitterServiceGrpc.TransmitterServiceStub asyncClient = TransmitterServiceGrpc.newStub(channel);

        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<TransmitterRequest> requestObserver = asyncClient.bloodLevelTransmitter(new StreamObserver<TransmitterResponse>() {
            @Override
            public void onNext(TransmitterResponse value) {
                //streaming response
                System.out.println("Glucose Level: " + value.getResult() + " mg/dL");
            }

            @Override
            public void onError(Throwable t) {
                //if got an error
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                //when the streaming is done
                System.out.println("Bidirectional Streaming successfully completed.");
                System.out.println();
                latch.countDown();
            }
        });
        //random generates numbers to simulate the glucose level
        int min=60;
        int max = 250;
        for(int i=0; i < 10; i++){
            int glucose_level = (int)Math.floor(Math.random()*(max-min+1)+min);
            glucoseArray[i] += glucose_level; //store this data into an array that will be used later in other methods
            //sending messages
            requestObserver.onNext(TransmitterRequest.newBuilder()
                    .setRequest(Transmitter.newBuilder().setGlucoseLevel(glucose_level)).build());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //client is done
        requestObserver.onCompleted();
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void ClientStreamingCall(ManagedChannel channel) {
        //create an asynchronous client
        MobileServiceGrpc.MobileServiceStub asyncClient = MobileServiceGrpc.newStub(channel);
        //this allows one Thread to wait for one or more Thread before it starts processing
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<AppRequest> requestObserver = asyncClient.app(new StreamObserver<AppResponse>() {
            @Override
            public void onNext(AppResponse value) {
                //we display the response
                try {
                    Thread.sleep(1000);
                    System.out.println("The highest blood level was: " + value.getResult() + " mg/dL");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(Throwable t) {
                //when we get an error the latch will count down and stop the streaming
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                //the streaming is done
                System.out.println("Client-side Streaming successfully completed.");
                System.out.println();
                latch.countDown();

            }
        });
        //sending the glucose Array data to server
        for (int j : glucoseArray) {
            requestObserver.onNext(AppRequest.newBuilder()
                    .setRequest(MobileApp.newBuilder()
                            .setGlucoseLevel(j)).build());
            System.out.println("Sending glucose level: " + j + " mg/dL");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        //we tell the server that client is done
        requestObserver.onCompleted();
        try { //this Thread will await 3 seconds until client is done
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void ServerStreamingCall(ManagedChannel channel) {
        //SERVER STREAMING
        //we prepare the request
        WatchServiceGrpc.WatchServiceBlockingStub stub = WatchServiceGrpc.newBlockingStub(channel);

        //getting the average from glucose Array data
        int sum = 0;
        int count = 0;
        for (int j : glucoseArray) {
            sum += j;
            count += 1;
        }

        double average = sum/count;

        try {
            Thread.sleep(1000);
            System.out.println("The blood sugar average is: " + average + " mg/dL");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //we stream the responses (in a blocking manner)
        stub.app(AverageRequest.newBuilder().setRequest(WatchApp.newBuilder().setGlucoseLevel(average).build()).build())
        .forEachRemaining(analysisResponse -> System.out.println(analysisResponse.getResult()));

        System.out.println("Server-side Streaming successfully completed.");

    }



}
