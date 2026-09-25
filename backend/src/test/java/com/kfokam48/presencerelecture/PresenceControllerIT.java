package com.kfokam48.presencerelecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresenceControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposeLesPresencesDuneSessionAvecLeurSource() throws Exception {
        String sessionBody = objectMapper.writeValueAsString(new CreateSessionRequest("Session liste présences", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();
        String code = objectMapper.readTree(created).get("code").asText();

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPresenceRequest(code, 1L))))
                .andExpect(status().isCreated());

        String presences = mockMvc.perform(get("/api/sessions/{id}/presences", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode rows = objectMapper.readTree(presences);
        assertThat(row(rows, 1L).get("nom").asText()).isEqualTo("Dupont");
        assertThat(row(rows, 1L).get("present").asBoolean()).isTrue();
        assertThat(row(rows, 1L).get("source").asText()).isEqualTo("ETUDIANT");
        assertThat(row(rows, 1L).get("marqueeAt").isTextual()).isTrue();
        assertThat(row(rows, 4L).get("present").asBoolean()).isFalse();
        assertThat(row(rows, 4L).get("source").isNull()).isTrue();
        assertThat(row(rows, 4L).get("marqueeAt").isNull()).isTrue();
    }

    @Test
    void enregistreUnePresenceEtRefuseUnDoublon() throws Exception {
        String sessionBody = objectMapper.writeValueAsString(new CreateSessionRequest("Session présence", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(created).get("code").asText();

        String presenceBody = objectMapper.writeValueAsString(new MarkPresenceRequest(code, 1L));
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presenceBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.etudiantId").value(1))
                .andExpect(jsonPath("$.source").value("ETUDIANT"));

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(presenceBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(jsonPath("$.message").isString());
    }

    private JsonNode row(JsonNode rows, long studentId) {
        for (JsonNode row : rows) {
            if (row.get("etudiantId").asLong() == studentId) {
                return row;
            }
        }
        throw new AssertionError("Étudiant absent du roster : " + studentId);
    }

    @Test
    void refuseUnCodeInconnu() throws Exception {
        String body = objectMapper.writeValueAsString(new MarkPresenceRequest("INCONNU", 1L));

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(jsonPath("$.message").isString());
    }
}
