package com.pet.requisition.application.port.out;
import com.pet.requisition.application.model.SystemParams;
public interface RequestLogPort { void incomingMessage(SystemParams systemParams); void warning(String message,Throwable exception); }
