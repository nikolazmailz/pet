package com.pet.requisition.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.requisition.application.handler.*;
import com.pet.requisition.application.port.out.*;
import com.pet.requisition.application.service.*;
import com.pet.requisition.application.usecase.dict.*;
import com.pet.requisition.application.usecase.pending.*;
import com.pet.requisition.infrastructure.adapter.*;
import com.pet.requisition.infrastructure.json.*;
import com.pet.requisition.infrastructure.kafka.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import com.pet.requisition.infrastructure.context.ThreadLocalRequestContextAdapter;
import com.pet.requisition.infrastructure.logging.Slf4jRequestLogAdapter;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {
    @Bean
    MessageCodecPort messageCodec(ObjectMapper m) {
        return new JacksonMessageCodecAdapter(m);
    }

    @Bean
    FileStoragePort fileStorage() {
        return new DemoFileStorageAdapter();
    }

    @Bean
    RolePort roles() {
        return new DemoRoleAdapter();
    }

    @Bean
    StaffPort staff() {
        return new DemoStaffAdapter();
    }

    @Bean
    RequestPersistencePort persistence() {
        return new InMemoryRequestPersistenceAdapter();
    }

    @Bean
    RequestContextPort context() {
        return new ThreadLocalRequestContextAdapter();
    }

    @Bean
    RequestLogPort requestLog() {
        return new Slf4jRequestLogAdapter();
    }

    @Bean
    PendingCandidatesUseCase pendingCandidatesUseCase() {
        return new PendingCandidatesUseCase();
    }

    @Bean
    PendingCandidatesHandler pendingCandidatesHandler(PendingCandidatesUseCase u) {
        return new PendingCandidatesHandler(u);
    }

    @Bean
    RequisitionDictUseCase requisitionDictUseCase() {
        return new RequisitionDictUseCase();
    }

    @Bean
    RequisitionDictHandler requisitionDictHandler(RequisitionDictUseCase u) {
        return new RequisitionDictHandler(u);
    }

    @Bean
    RequestHandlerResolver resolver(List<RequestHandler<?, ?>> handlers) {
        return new RequestHandlerResolver(handlers);
    }

    @Bean
    RequestHandlerExecutor executor(MessageCodecPort c, RequestHandlerResolver r) {
        return new RequestHandlerExecutor(c, r);
    }

    @Bean
    PreparedRequestService prepared(MessageCodecPort c, FileStoragePort f, RolePort r, StaffPort s, RequestPersistencePort p, RequestContextPort ctx, RequestLogPort l) {
        return new PreparedRequestService(c, f, r, s, p, ctx, l);
    }

    @Bean
    ResponseProcessingService response(MessageCodecPort c, FileStoragePort f, RequestPersistencePort p, @Value("${app.request-reply.max-inner-message-size-kb}") int max) {
        return new ResponseProcessingService(c, f, p, max);
    }

    @Bean
    MessageProcessingService processing(MessageCodecPort c, PreparedRequestService p, RequestHandlerExecutor e, ResponseProcessingService r) {
        return new MessageProcessingService(c, p, e, r);
    }

    @Bean
    ResponseKafkaProducer producer(KafkaTemplate<String, String> k, @Value("${app.kafka.response-topic}") String topic) {
        return new ResponseKafkaProducer(k, topic);
    }

    @Bean
    RequisitionKafkaListener listener(MessageProcessingService p, ResponseKafkaProducer producer, RequestContextPort c) {
        return new RequisitionKafkaListener(p, producer, c);
    }
}
