package ru.ntdev.srhr.requisition.pendingcandidates.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesData;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesResponse;
import ru.ntdev.srhr.requisition.pendingcandidates.client.MasterDataIntegrationClient;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidate;
import ru.ntdev.srhr.requisition.pendingcandidates.mapper.PendingCandidateMapper;
import ru.ntdev.srhr.requisition.pendingcandidates.repository.PendingCandidateRepository;

import java.util.List;
import java.util.Optional;

/**
 * Полная замена снимка кандидатов по approver_pernr: fetch из Е-стафф ->
 * delete -> insert. HTTP-запрос выполняется ДО открытия транзакции: не держим
 * соединение с БД и advisory lock на время сетевого вызова; при ошибке
 * интеграции локальные данные не затрагиваются.
 */
@Service
@RequiredArgsConstructor
public class PendingCandidatesRefreshService {

    private final MasterDataIntegrationClient integrationClient;
    private final PendingCandidateRepository repository;
    private final PendingCandidateMapper mapper;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    public void refresh(String pernr) {
        EstaffPendingCandidatesResponse response = integrationClient.fetchPendingCandidates(pernr);
        transactionTemplate.executeWithoutResult(tx -> replaceData(pernr, response));
    }

    private void replaceData(String pernr, EstaffPendingCandidatesResponse response) {
        // Сериализация конкурентных refresh одного pernr; снимается при commit/rollback
        acquireAdvisoryLock(pernr);

        repository.deleteByApproverPernr(pernr);

        List<EstaffCandidate> candidates = Optional.ofNullable(response.data())
                .map(EstaffPendingCandidatesData::candidates)
                .orElse(List.of());

        List<PendingCandidate> entities = candidates.stream()
                .map(candidate -> mapper.toEntity(candidate, pernr))
                .toList();

        repository.saveAll(entities);
    }

    private void acquireAdvisoryLock(String pernr) {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtext(:pernr))")
                .setParameter("pernr", pernr)
                .getSingleResult();
    }
}
