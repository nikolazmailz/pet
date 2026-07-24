package ru.ntdev.srhr.mdi.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ntdev.srhr.mdi.application.GetPendingCandidatesUseCase;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesSnapshotResponse;
import ru.ntdev.srhr.pending.contracts.PernrRequestEnvelope;

@RestController
@RequestMapping("/pending-candidates")
public class PendingCandidatesController {
    private final GetPendingCandidatesUseCase useCase;

    public PendingCandidatesController(GetPendingCandidatesUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public PendingCandidatesSnapshotResponse get(@Valid @RequestBody PernrRequestEnvelope request) {
        return useCase.execute(request);
    }
}
