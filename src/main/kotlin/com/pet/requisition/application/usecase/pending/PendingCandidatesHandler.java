package com.pet.requisition.application.usecase.pending;
import ru.ntdev.srhr.ms.requisition.application.handler.RequestHandler;
import ru.ntdev.srhr.ms.requisition.application.model.PreparedRequest;
public class PendingCandidatesHandler implements RequestHandler<PendingCandidatesRequest,PendingCandidatesResponse> {
    private final PendingCandidatesUseCase useCase;
    public PendingCandidatesHandler(PendingCandidatesUseCase useCase){this.useCase=useCase;}
    public String requestType(){return "requisition_pending_candidates";}
    public Class<PendingCandidatesRequest> requestClass(){return PendingCandidatesRequest.class;}
    public PendingCandidatesResponse handle(PreparedRequest<PendingCandidatesRequest> request){return useCase.execute(request.systemParams(),request.payload());}
}
