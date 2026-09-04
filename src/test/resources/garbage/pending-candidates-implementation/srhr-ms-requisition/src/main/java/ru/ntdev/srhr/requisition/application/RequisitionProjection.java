package ru.ntdev.srhr.requisition.application;

import ru.ntdev.srhr.pending.contracts.RequisitionView;

record RequisitionProjection(String vacancyId, long requisitionPk, RequisitionView view) {}
