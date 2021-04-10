package CGM_Server;

import com.proto.mobileApp.AppRequest;
import com.proto.mobileApp.AppResponse;
import com.proto.mobileApp.MobileServiceGrpc;
import com.proto.transmitter.Transmitter;
import com.proto.transmitter.TransmitterRequest;
import com.proto.transmitter.TransmitterResponse;
import com.proto.transmitter.TransmitterServiceGrpc;
import io.grpc.stub.StreamObserver;

public class TransmitterServiceImpl extends TransmitterServiceGrpc.TransmitterServiceImplBase{
    @Override
    public void transmitter(TransmitterRequest request, StreamObserver<TransmitterResponse> responseObserver) {
        //extract the fields we need
        Transmitter transmitter = request.getRequest();
        int result = transmitter.getGlucoseLevel();

        TransmitterResponse response = TransmitterResponse.newBuilder()
                .setResult(result)
                .build();
        //send the response
        responseObserver.onNext(response);

        //complete the RPC
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<TransmitterRequest> bloodLevelTransmitter(StreamObserver<TransmitterResponse> responseObserver) {
        StreamObserver<TransmitterRequest> requestObserver = new StreamObserver<TransmitterRequest>() {
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

            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
        return requestObserver;
    }
}

