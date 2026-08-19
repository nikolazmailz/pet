package com.pet.requisition.application.usecase.pending;

import com.pet.requisition.application.model.*;
import java.util.List;

public class PendingCandidatesUseCase {
    public PendingCandidatesResponse execute(SystemParams systemParams, PendingCandidatesRequest request) {
        return new PendingCandidatesResponse(request.page(), request.pageSize(), 1, List.of("Demo Candidate"));
    }
}
