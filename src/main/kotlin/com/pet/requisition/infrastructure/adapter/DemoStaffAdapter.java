package com.pet.requisition.infrastructure.adapter;
import ru.ntdev.srhr.ms.requisition.application.port.out.StaffPort;
public class DemoStaffAdapter implements StaffPort {
    public String getTabNumber(String adLogin,String sessionId,String traceId){
        // REAL: bscsIntegrationService.getStaffByLogin(...).getId()
        return "000001";
    }
}
