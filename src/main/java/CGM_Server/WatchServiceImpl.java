package CGM_Server;

import com.proto.watchApp.AnalysisResponse;
import com.proto.watchApp.AverageRequest;
import com.proto.watchApp.WatchServiceGrpc;
import io.grpc.stub.StreamObserver;

public class WatchServiceImpl extends WatchServiceGrpc.WatchServiceImplBase {
    @Override
    public void app(AverageRequest request, StreamObserver<AnalysisResponse> responseObserver) {

        int average = request.getRequest().getGlucoseLevel();
        String[] low = new String[] {"Your blood sugar level is too low.It can be caused by: ", "Miss or forget to take insulin; ","Are not taking enough insulin; ",
                "Eat more carbohydrate foods than usual"};
        String[] high = new String[] {"Your blood sugar level is too high. It can be caused by: ","Take too much insulin","Eat less carbohydrate than usual","Leave too long between meals"};
        String[] normal = new String[] {"Your blood sugar level is good. But you still should take some precautions: ", "Be aware of hypo symptoms and treat as necessary;",
                "Always make sure you, your child or whoever is caring for them has access to quick acting carbs;","Make sure your child carries diabetes identification"};
        String result = "this is a test of result that will be printed";
        if(average < 80){
            for (String s : low) {
                responseObserver.onNext(AnalysisResponse.newBuilder().setResult(s).build());
            }


        }else if(average > 80 && average < 150){
            for (String s : normal) {
                responseObserver.onNext(AnalysisResponse.newBuilder().setResult(s).build());
            }

        }else{
            for (String s : high) {
                responseObserver.onNext(AnalysisResponse.newBuilder().setResult(s).build());
            }
        }

        responseObserver.onCompleted();
    }
}
