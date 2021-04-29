package CGM_Server;

import io.grpc.Server;
import io.grpc.ServerBuilder;


import java.io.IOException;
import java.util.Properties;

public class CGMServer {
    public static void main(String[] args) {

        try{
            //setting properties for Transmitter
            TransmitterServiceImpl transmitterService = new TransmitterServiceImpl();
            Properties transmitterProp = transmitterService.getProperties();
            //registering service
            transmitterService.registerService(transmitterProp);
            int transmitterPort = Integer.parseInt(transmitterProp.getProperty("service_port"));

            //setting properties  for mobile app
            AppServiceImpl appService = new AppServiceImpl();
            Properties appProp = appService.getProperties();
            //registering service
            appService.registerService(appProp);
            int appPort = Integer.parseInt(appProp.getProperty("service_port"));

            //setting properties  for app
            WatchServiceImpl watchService = new WatchServiceImpl();
            Properties watchProp = watchService.getProperties();
            //registering service
            watchService.registerService(watchProp);
            int watchPort = Integer.parseInt(watchProp.getProperty("service_port"));
            //build the server1 and add all services
            Server server1 = ServerBuilder.forPort(transmitterPort).addService(new TransmitterServiceImpl())
                    .build();
            server1.start();
            System.out.println("Transmitter Service is running...");

            //build the server2 and add all services
            Server server2 = ServerBuilder.forPort(appPort).addService(new AppServiceImpl())
                    .build();
            server2.start();
            System.out.println("Mobile App Service is running...");

            //build the server2 and add all services
            Server server3 = ServerBuilder.forPort(watchPort).addService(new WatchServiceImpl())
                    .build();
            server3.start();
            System.out.println("Watch App Service is running...");

            //this will shutdown the server1
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                System.out.println("Received shutdown request");
                server1.shutdown();
                System.out.println("Server stopped!");

            }));

            //this will shutdown the server2
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                System.out.println("Received shutdown request");
                server2.shutdown();
                System.out.println("Server stopped!");

            }));

            //this will shutdown the server2
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                System.out.println("Received shutdown request");
                server3.shutdown();
                System.out.println("Server stopped!");

            }));

            //will wait until the program is done
            server1.awaitTermination();

            //will wait until the program is done
            server2.awaitTermination();

            //will wait until the program is done
            server3.awaitTermination();

        }catch(IOException | InterruptedException e){
            e.printStackTrace();
        }

    }
}
