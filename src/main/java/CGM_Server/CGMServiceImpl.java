package CGM_Server;

import com.proto.transmitter.Transmitter;
import com.proto.transmitter.TransmitterRequest;
import com.proto.transmitter.TransmitterResponse;
import com.proto.transmitter.TransmitterServiceGrpc;
import io.grpc.stub.StreamObserver;

public class CGMServiceImpl extends TransmitterServiceGrpc.TransmitterServiceImplBase {
    @Override
    public void transmitter(TransmitterRequest request, StreamObserver<TransmitterResponse> responseObserver) {
        //extract the fields we need
        Transmitter transmitter = request.getRequest();
        Integer glucoselevel = transmitter.getGlucoseLevel();

        //create the response
        Integer result = glucoselevel;
        TransmitterResponse response = TransmitterResponse.newBuilder()
                .setResult(result)
                .build();
        //send the response
        responseObserver.onNext(response);

        //complete the RPC
        responseObserver.onCompleted();
    }
}
