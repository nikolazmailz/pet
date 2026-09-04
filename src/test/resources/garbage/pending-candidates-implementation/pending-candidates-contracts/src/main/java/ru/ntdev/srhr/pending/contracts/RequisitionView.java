package ru.ntdev.srhr.pending.contracts;

import java.util.List;

public record RequisitionView(
        String guid,
        String number,
        String positionType,
        StaffPositionView staffPosition,
        String structUnitName,
        String structUnitId,
        List<StructUnitPathView> structUnitPathList
) {
    public RequisitionView {
        structUnitPathList = structUnitPathList == null ? List.of() : List.copyOf(structUnitPathList);
    }
}
