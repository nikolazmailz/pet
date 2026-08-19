package com.pet.requisition.application.usecase.dict;

import com.pet.requisition.application.model.*;
import java.util.Map;

public class RequisitionDictUseCase {
    public Map<String, String> execute(SystemParams p) {
        return Map.of("status", "ok");
    }
}
