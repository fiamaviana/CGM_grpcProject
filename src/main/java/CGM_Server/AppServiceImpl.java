package CGM_Server;

import com.proto.mobileApp.AppRequest;
import com.proto.mobileApp.AppResponse;
import com.proto.mobileApp.MobileServiceGrpc;
import io.grpc.stub.StreamObserver;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

public class AppServiceImpl extends MobileServiceGrpc.MobileServiceImplBase{


    public Properties getProperties() {

        Properties prop = null;

        try (InputStream input = new FileInputStream("src/main/properties/app.properties")) {

            prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.out.println("Mobile App Service properties ...");
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

            String service_type = prop.getProperty("service_type") ;//"_http._tcp.local.";
            String service_name = prop.getProperty("service_name")  ;// "example";
            // int service_port = 1234;
            int service_port = Integer.parseInt( prop.getProperty("service_port") );// #.50052;


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


    @Override
    public StreamObserver<AppRequest> app(StreamObserver<AppResponse> responseObserver) {
        StreamObserver<AppRequest> requestObserver = new StreamObserver<AppRequest>() {
            ArrayList<Integer> glucoseLevelArrayList = new ArrayList<>();
            int result = 0;
            @Override
            public void onNext(AppRequest value) {
                //client sends a message
                result = value.getRequest().getGlucoseLevel();
                System.out.println("glucose result: "+result);
                //stores the values into another array
                glucoseLevelArrayList.add(result);
            }

            @Override
            public void onError(Throwable t) {
                //client sends an error
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                //clients is done sending data
                Integer max = Collections.max(glucoseLevelArrayList);
                responseObserver.onNext(
                        AppResponse.newBuilder()
                                .setResult(max)
                                .build()
                );
                responseObserver.onCompleted();
            }
        };
        return requestObserver;
    }
}


