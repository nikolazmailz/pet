package com.pet.infra;

import com.pet.requestreply.application.handler.RequestHandler;
import com.pet.requestreply.application.model.PreparedRequest;

public class PendingCandidatesHandler
        implements RequestHandler<PendingCandidatesRequest, PendingCandidatesResponse> {

    private final PendingCandidatesUseCase useCase;

    public PendingCandidatesHandler(PendingCandidatesUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public String requestType() {
        return "requisition_pending_candidates";
    }

    @Override
    public Class<PendingCandidatesRequest> requestClass() {
        return PendingCandidatesRequest.class;
    }

    @Override
    public PendingCandidatesResponse handle(
            PreparedRequest<PendingCandidatesRequest> request
    ) {
        return useCase.execute(
                request.systemParams(),
                request.payload()
        );
    }
}
