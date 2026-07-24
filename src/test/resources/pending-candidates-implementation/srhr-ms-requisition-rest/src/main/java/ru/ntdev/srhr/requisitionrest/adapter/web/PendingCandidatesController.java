package ru.ntdev.srhr.requisitionrest.adapter.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesApiResponse;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPage;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPageRequest;
import ru.ntdev.srhr.requisitionrest.application.CurrentUserPernrResolver;
import ru.ntdev.srhr.requisitionrest.application.PendingCandidatesRequestReplyGateway;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/pending-candidates")
public class PendingCandidatesController {
    private final CurrentUserPernrResolver pernrResolver;
    private final PendingCandidatesRequestReplyGateway gateway;

    public PendingCandidatesController(CurrentUserPernrResolver pernrResolver,
                                       PendingCandidatesRequestReplyGateway gateway) {
        this.pernrResolver = pernrResolver;
        this.gateway = gateway;
    }

    @PostMapping
    public PendingCandidatesApiResponse get(@Valid @RequestBody PendingCandidatesPageRequest request,
                                            HttpServletRequest httpRequest) {
        String traceId = Optional.ofNullable(httpRequest.getHeader("X-Trace-Id"))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        String pernr = pernrResolver.resolve(httpRequest, traceId);
        PendingCandidatesPage page = gateway.execute(pernr, traceId, request);
        return new PendingCandidatesApiResponse(page);
    }
}
