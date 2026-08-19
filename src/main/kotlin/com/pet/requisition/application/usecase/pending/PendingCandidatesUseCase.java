package com.pet.requisition.application.usecase.pending;
import ru.ntdev.srhr.ms.requisition.application.model.SystemParams;
import java.util.List;
public class PendingCandidatesUseCase {
    public PendingCandidatesResponse execute(SystemParams systemParams,PendingCandidatesRequest request){
        return new PendingCandidatesResponse(request.page(),request.pageSize(),1,List.of("Demo Candidate"));
    }
}
