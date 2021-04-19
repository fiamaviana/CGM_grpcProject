package CGM_Server;

import com.proto.transmitter.Transmitter;
import com.proto.transmitter.TransmitterRequest;
import com.proto.transmitter.TransmitterResponse;
import com.proto.transmitter.TransmitterServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

public class TransmitterServiceImpl extends TransmitterServiceGrpc.TransmitterServiceImplBase{
    //jmDNS
    public Properties getProperties() {

        Properties prop = null;

        try (InputStream input = new FileInputStream("src/main/properties/transmitter.properties")) {

            prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.out.println("Transmitter Service properties ...");
            System.out.println("\t service_type: " + prop.getProperty("service_type"));
            System.out.println("\t service_name: " +prop.getProperty("service_name"));
            System.out.println("\t service_description: " +prop.getProperty("service_description"));
            System.out.println("\t service_port: " +prop.getProperty("service_port"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return prop;
    }


    public  void registerService(Properties prop) {

        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            String service_type = prop.getProperty("service_type") ;//"_transmitter._tcp.local."
            String service_name = prop.getProperty("service_name")  ;// "transmitter";
            // int service_port = 1234;
            int service_port = Integer.parseInt( prop.getProperty("service_port") );// #50051;


            String service_description_properties = prop.getProperty("service_description")  ;//"path=index.html";

            // Register a service
            ServiceInfo serviceInfo = ServiceInfo.create(service_type, service_name, service_port, service_description_properties);
            jmdns.registerService(serviceInfo);

            System.out.printf("registering service with type %s and name %s \n", service_type, service_name);

            // Wait a bit
            Thread.sleep(1000);

            // Unregister all services
            //jmdns.unregisterAllServices();

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    //UNARY STREAM
    @Override
    public void transmitter(TransmitterRequest request, StreamObserver<TransmitterResponse> responseObserver) {
        //extract the fields we need
        Transmitter transmitter = request.getRequest();
        int result = transmitter.getGlucoseLevel();
        if(result >= 0){//check if the number is positive to build a response
            TransmitterResponse response = TransmitterResponse.newBuilder()
                    .setResult(result)
                    .build();
            //send the response
            responseObserver.onNext(response);

            //complete the RPC
            responseObserver.onCompleted();
        }else{
            //handling an exception in case the result is not a positive number
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("The number being sent is not positive")
                            .augmentDescription("Number sent: " + result)
                            .asRuntimeException()
            );
        }

    }
    //BINARY STREAM
    @Override
    public StreamObserver<TransmitterRequest> bloodLevelTransmitter(StreamObserver<TransmitterResponse> responseObserver) {
        //create an object of the request
        StreamObserver<TransmitterRequest> requestObserver = new StreamObserver<TransmitterRequest>() {
            //create and send the response
            @Override
            public void onNext(TransmitterRequest value) {
                int response = value.getRequest().getGlucoseLevel();
                TransmitterResponse thisresponse = TransmitterResponse.newBuilder()
                        .setResult(response)
                        .build();

                responseObserver.onNext(thisresponse);
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }
            //send a message that this service is completed
            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
        return requestObserver;
    }
}

