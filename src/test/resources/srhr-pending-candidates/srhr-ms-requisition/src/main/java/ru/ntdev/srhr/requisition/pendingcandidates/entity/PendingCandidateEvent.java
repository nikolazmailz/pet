package ru.ntdev.srhr.requisition.pendingcandidates.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "pending_candidates_events")
@Getter
@Setter
@NoArgsConstructor
public class PendingCandidateEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pending_candidate_id", nullable = false)
    private PendingCandidate candidate;

    @Column(name = "event_code", nullable = false)
    private String eventCode;

    /** Из Е-стафф приходит epoch millis UTC; в БД — TIMESTAMPTZ. */
    @Column(name = "status_date", nullable = false)
    private Instant statusDate;

    @Column(name = "days")
    private Integer days;

    @Column(name = "expiration_zone")
    private Integer expirationZone;
}
