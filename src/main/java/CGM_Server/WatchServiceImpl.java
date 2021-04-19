package CGM_Server;

import com.proto.watchApp.AnalysisResponse;
import com.proto.watchApp.AverageRequest;
import com.proto.watchApp.WatchServiceGrpc;
import io.grpc.stub.StreamObserver;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

public class WatchServiceImpl extends WatchServiceGrpc.WatchServiceImplBase {


    public Properties getProperties() {

        Properties prop = null;

        try (InputStream input = new FileInputStream("src/main/properties/watch.properties")) {

            prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.out.println();
            System.out.println("Watch App Service properties ...");
            System.out.println("\t service_type: " + prop.getProperty("service_type"));
            System.out.println("\t service_name: " +prop.getProperty("service_name"));
            System.out.println("\t service_description: " +prop.getProperty("service_description"));
            System.out.println("\t service_port: " +prop.getProperty("service_port"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return prop;
    }


    public void registerService(Properties prop) {

        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            String service_type = prop.getProperty("service_type") ;//"_http._tcp.local.";
            String service_name = prop.getProperty("service_name")  ;// "example";
            // int service_port = 1234;
            int service_port = Integer.parseInt( prop.getProperty("service_port") );// #.50053;


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
    public void app(AverageRequest request, StreamObserver<AnalysisResponse> responseObserver) {

        double average = request.getRequest().getGlucoseLevel();
        String[] low = new String[] {"Your blood sugar level is too low.It can be caused by: ", "Miss or forget to take insulin; ","Are not taking enough insulin; ",
                "Eat more carbohydrate foods than usual"};
        String[] high = new String[] {"Your blood sugar level is too high. It can be caused by: ","Take too much insulin","Eat less carbohydrate than usual","Leave too long between meals"};
        String[] normal = new String[] {"Your blood sugar level is good. But you still should take some precautions: ", "Be aware of hypo symptoms and treat as necessary;",
                "Always make sure you, your child or whoever is caring for them has access to quick acting carbs;","Make sure your child carries diabetes identification"};
        String result = "this is a test of result that will be printed";

        //this statement will display messages according to the blood sugar level average
        if(average < 80){
            //if the average is less than 80 it will display messages about low blood sugar level
            for (String s : low) {
                responseObserver.onNext(AnalysisResponse.newBuilder().setResult(s).build());
            }

        }else if(average > 80 && average < 150){
            //if it is between 80 and 150 it will display messages about normal blood sugar level
            for (String s : normal) {
                responseObserver.onNext(AnalysisResponse.newBuilder().setResult(s).build());
            }

        }else{ //if it is greater than 150 it will display messages about high blood sugar level
            for (String s : high) {
                responseObserver.onNext(AnalysisResponse.newBuilder().setResult(s).build());
            }
        }

        responseObserver.onCompleted();
    }
}
