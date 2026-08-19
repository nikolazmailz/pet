package com.pet.requisition.application.usecase.pending;
import java.util.List;
public record PendingCandidatesRequest(int page,int pageSize,String fullName,List<String> eventCodes) {}
