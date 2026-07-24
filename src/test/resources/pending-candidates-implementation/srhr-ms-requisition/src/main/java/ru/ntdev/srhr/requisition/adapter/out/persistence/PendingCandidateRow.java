package ru.ntdev.srhr.requisition.adapter.out.persistence;

public record PendingCandidateRow(long id, String candidateId, String vacancyId, String fullName) {}
