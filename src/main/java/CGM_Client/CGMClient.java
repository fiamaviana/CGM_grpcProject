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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        BidiStreamingCall(channel);
        ClientStreamingCall(channel);
        ServerStreamingCall(channel);
        System.out.println();
        System.out.println("Shutting down channel");
        channel.shutdown();

    }

    private void ServerStreamingCall(ManagedChannel channel) {
        WatchServiceGrpc.WatchServiceBlockingStub stub = WatchServiceGrpc.newBlockingStub(channel);

        int number = 160;

        stub.app(AverageRequest.newBuilder().setRequest(WatchApp.newBuilder().setGlucoseLevel(number).build()).build())
        .forEachRemaining(analysisResponse -> System.out.println(analysisResponse.getResult()));

        System.out.println("Server-side Streaming successfully completed.");

    }

    private void ClientStreamingCall(ManagedChannel channel) {
        MobileServiceGrpc.MobileServiceStub asyncClient = MobileServiceGrpc.newStub(channel);

        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<AppRequest> requestObserver = asyncClient.app(new StreamObserver<AppResponse>() {
            @Override
            public void onNext(AppResponse value) {
                System.out.println("The highest blood level was: " + value.getResult());
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Client-side Streaming successfully completed.");
                System.out.println();
                latch.countDown();

            }
        });
        requestObserver.onNext(AppRequest.newBuilder().setRequest(MobileApp.newBuilder().setGlucoseLevel(140)).build());
        requestObserver.onNext(AppRequest.newBuilder().setRequest(MobileApp.newBuilder().setGlucoseLevel(160)).build());
        requestObserver.onNext(AppRequest.newBuilder().setRequest(MobileApp.newBuilder().setGlucoseLevel(130)).build());
        requestObserver.onNext(AppRequest.newBuilder().setRequest(MobileApp.newBuilder().setGlucoseLevel(120)).build());

        requestObserver.onCompleted();
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
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
                System.out.println("Glucose Level: " + value.getResult());
            }

            @Override
            public void onError(Throwable t) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Bidirectional Streaming successfully completed.");
                System.out.println();
                latch.countDown();
            }
        });
        Arrays.asList(140,160,130,120).forEach(
                bloodlevel -> requestObserver.onNext(TransmitterRequest.newBuilder()
                        .setRequest(Transmitter.newBuilder().setGlucoseLevel(bloodlevel)).build())
        );
        requestObserver.onCompleted();
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void UnaryCall(ManagedChannel channel) {
        //created a transmitter service client (blocking - synchronous)
        TransmitterServiceGrpc.TransmitterServiceBlockingStub transmitterClient = TransmitterServiceGrpc.newBlockingStub(channel);
        //created a protocol buffer transmitter message
        Transmitter transmitter = Transmitter.newBuilder()
                .setGlucoseLevel(115)
                .build();
        //do the same for a transmitter Request
        TransmitterRequest request = TransmitterRequest.newBuilder()
                .setRequest(transmitter)
                .build();
        //call the RPC and get back a GreetResponse (protocol buffers)
        TransmitterResponse response = transmitterClient.transmitter(request);

        System.out.println("Glucose level: " + response.getResult());
        System.out.println("Unary Streaming successfully completed.");
        System.out.println();
    }

}
