package ru.ntdev.srhr.masterdataintegration.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesData;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesResponse;
import ru.ntdev.srhr.masterdataintegration.client.EstaffClient;
import ru.ntdev.srhr.masterdataintegration.exception.EstaffIntegrationException;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PendingCandidatesController.class)
@DisplayName("POST /pending-candidates (master-data-integration)")
class PendingCandidatesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EstaffClient estaffClient;

    @Test
    @DisplayName("успешный passthrough ответа Е-стафф")
    void passthroughOk() throws Exception {
        when(estaffClient.fetchPendingCandidates("12345678")).thenReturn(
                new EstaffPendingCandidatesResponse(new EstaffPendingCandidatesData(
                        List.of(new EstaffCandidate("1", "2", "Иванов", List.of())))));

        mockMvc.perform(post("/pending-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "request": { "pernr": "12345678" } }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates[0].fullName").value("Иванов"));
    }

    @Test
    @DisplayName("пустой pernr -> 400")
    void blankPernrRejected() throws Exception {
        mockMvc.perform(post("/pending-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "request": { "pernr": "" } }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ошибка Е-стафф -> 502 с телом ошибки")
    void estaffErrorMappedTo502() throws Exception {
        when(estaffClient.fetchPendingCandidates(anyString()))
                .thenThrow(new EstaffIntegrationException("timeout"));

        mockMvc.perform(post("/pending-candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "request": { "pernr": "12345678" } }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("ESTAFF_UNAVAILABLE"));
    }
}
