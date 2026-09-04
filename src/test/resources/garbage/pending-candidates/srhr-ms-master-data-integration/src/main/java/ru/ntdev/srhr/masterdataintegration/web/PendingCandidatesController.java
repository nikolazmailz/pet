package ru.ntdev.srhr.masterdataintegration.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesResponse;
import ru.ntdev.srhr.masterdataintegration.client.EstaffClient;

/**
 * Тонкий прокси к SAPPO: passthrough без трансформаций.
 */
@RestController
@RequiredArgsConstructor
public class PendingCandidatesController {

    private final EstaffClient estaffClient;

    @PostMapping("/garbage/pending-candidates")
    public EstaffPendingCandidatesResponse pendingCandidates(
            @RequestBody EstaffPendingCandidatesRequest request) {
        if (request.request() == null || request.request().pernr() == null
                || request.request().pernr().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request.pernr обязателен");
        }
        return estaffClient.fetchPendingCandidates(request.request().pernr());
    }
}
