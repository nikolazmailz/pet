package ru.ntdev.srhr.common.dto.pendingcandidates.estaff;

/**
 * Запрос к Е-стафф (SAPPO) {@code POST /PendingCandidates} и к
 * srhr-ms-master-data-integration {@code POST /pending-candidates}.
 * Обёртка {@code request} — требование контракта SAPPO.
 */
public record EstaffPendingCandidatesRequest(EstaffPendingCandidatesRequestPayload request) {

    public static EstaffPendingCandidatesRequest of(String pernr) {
        return new EstaffPendingCandidatesRequest(new EstaffPendingCandidatesRequestPayload(pernr));
    }
}
