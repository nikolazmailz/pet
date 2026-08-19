package com.pet.requisition.application.port.out;
import java.util.List;
public interface RolePort { List<String> getRoles(String adLogin,String channel,String sessionId,String traceId); }
