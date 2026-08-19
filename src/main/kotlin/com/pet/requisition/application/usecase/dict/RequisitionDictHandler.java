package com.pet.requisition.application.usecase.dict;
import ru.ntdev.srhr.ms.requisition.application.handler.RequestHandler;
import ru.ntdev.srhr.ms.requisition.application.model.PreparedRequest;
import java.util.Map;
public class RequisitionDictHandler implements RequestHandler<Void,Map<String,String>> {
    private final RequisitionDictUseCase useCase;
    public RequisitionDictHandler(RequisitionDictUseCase useCase){this.useCase=useCase;}
    public String requestType(){return "requisition_dict";}
    public Class<Void> requestClass(){return Void.class;}
    public Map<String,String> handle(PreparedRequest<Void> request){return useCase.execute(request.systemParams());}
}
