package ru.ntdev.srhr.common.dto.pendingcandidates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RequisitionShortDto(
        String guid,
        String number,
        String positionType,
        StaffPositionDto staffPosition,
        String structUnitName,
        String structUnitId,
        List<StructUnitPathDto> structUnitPathList) {
}
