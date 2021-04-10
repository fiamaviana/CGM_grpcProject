package CGM_Server;

import com.proto.mobileApp.AppRequest;
import com.proto.mobileApp.AppResponse;
import com.proto.mobileApp.MobileServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Collections;

public class AppServiceImpl extends MobileServiceGrpc.MobileServiceImplBase{

    @Override
    public StreamObserver<AppRequest> app(StreamObserver<AppResponse> responseObserver) {
        StreamObserver<AppRequest> requestObserver = new StreamObserver<AppRequest>() {
            ArrayList<Integer> glucoseLevelArrayList = new ArrayList<>();
            int result = 0;
            @Override
            public void onNext(AppRequest value) {
                //client sends a message
                result = value.getRequest().getGlucoseLevel();
                glucoseLevelArrayList.add(result);
            }

            @Override
            public void onError(Throwable t) {
                //client sends an error
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


