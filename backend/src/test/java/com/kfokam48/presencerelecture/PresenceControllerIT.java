package com.kfokam48.presencerelecture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        mockMvc.perform(get("/api/sessions/{id}/presences", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].etudiantId").value(1))
                .andExpect(jsonPath("$[0].nom").value("Dupont"))
                .andExpect(jsonPath("$[0].present").value(true))
                .andExpect(jsonPath("$[0].source").value("ETUDIANT"))
                .andExpect(jsonPath("$[0].marqueeAt").isString());
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
