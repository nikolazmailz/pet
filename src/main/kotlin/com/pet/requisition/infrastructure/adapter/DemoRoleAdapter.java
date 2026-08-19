package com.pet.requisition.infrastructure.adapter;
import ru.ntdev.srhr.ms.requisition.application.port.out.RolePort;
import java.util.List;
public class DemoRoleAdapter implements RolePort {
    public List<String> getRoles(String adLogin,String channel,String sessionId,String traceId){
        // REAL: tsrmIntegrationService.getRole(...).getRoles().stream().map(ActorRoleDto::getRoleId).toList()
        return List.of("MSS_STANDARD");
    }
}
