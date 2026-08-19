package com.pet.requisition.application.usecase.pending;
import java.util.List;
public record PendingCandidatesResponse(int page,int pageSize,long count,List<String> candidates) {}
