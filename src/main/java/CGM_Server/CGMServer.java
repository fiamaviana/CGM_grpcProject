package CGM_Server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class CGMServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Server running...");

        Server server = ServerBuilder.forPort(50051)
                .addService(new TransmitterServiceImpl()).addService(new AppServiceImpl()).addService(new WatchServiceImpl())
                .build();
        server.start();
        //this will shutdown the server
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            System.out.println("Received shutdown request");
            server.shutdown();
            System.out.println("Server stopped!");

        }));
        //will wait until the program is done
        server.awaitTermination();
    }
}
