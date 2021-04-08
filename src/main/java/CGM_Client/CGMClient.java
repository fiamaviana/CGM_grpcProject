package CGM_Client;

import com.proto.transmitter.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class CGMClient {
    public static void main(String[] args) {
        System.out.println("gRPC client is running");
        CGMClient main = new CGMClient();
        main.run();
    }

    private void run(){
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        UnaryCall(channel);
        //doServerStreamingCall(channel);
        //doClientStreamingCall(channel);
        //doBidiStreamingCall(channel);
        System.out.println("Shutting down channel");
        channel.shutdown();

    }

    private void UnaryCall(ManagedChannel channel) {
        //created a transmitter service client (blocking - synchronous)
        TransmitterServiceGrpc.TransmitterServiceBlockingStub transmitterClient = TransmitterServiceGrpc.newBlockingStub(channel);
        //created a protocol buffer transmitter message
        Transmitter transmitter = Transmitter.newBuilder()
                .setGlucoseLevel(150)
                .build();
        //do the same for a transmitter Request
        TransmitterRequest request = TransmitterRequest.newBuilder()
                .setRequest(transmitter)
                .build();
        //call the RPC and get back a GreetResponse (protocol buffers)
        TransmitterResponse response = transmitterClient.transmitter(request);

        System.out.println("Glucose level: " + response.getResult());
    }

}
