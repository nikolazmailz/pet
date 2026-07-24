package ru.ntdev.srhr.requisition.pendingcandidates.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * equals/hashCode намеренно не переопределены: сущности не хранятся в Set
 * и не сравниваются между собой; identity-семантика достаточна и не создаёт
 * проблем с Hibernate-прокси и изменяемым id.
 */
@Entity
@Table(name = "pending_candidates")
@Getter
@Setter
@NoArgsConstructor
public class PendingCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Внешний идентификатор вакансии из Е-стафф; НЕ ссылка на внутреннюю vacancy. */
    @Column(name = "vacancy_id", nullable = false)
    private String vacancyId;

    @Column(name = "candidate_id", nullable = false)
    private String candidateId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "approver_pernr", nullable = false)
    private String approverPernr;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("statusDate ASC")
    private List<PendingCandidateEvent> events = new ArrayList<>();

    public void addEvent(PendingCandidateEvent event) {
        events.add(event);
        event.setCandidate(this);
    }
}
