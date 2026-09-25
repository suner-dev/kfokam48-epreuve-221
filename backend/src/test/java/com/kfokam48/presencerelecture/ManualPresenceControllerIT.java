package com.kfokam48.presencerelecture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.presence.api.ManualPresenceRequest;
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
class ManualPresenceControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void ajouteUnePresenceFormateur() throws Exception {
        String sessionBody = objectMapper.writeValueAsString(new CreateSessionRequest("Session manuelle", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();
        String body = objectMapper.writeValueAsString(new ManualPresenceRequest(sessionId, 1L));

        mockMvc.perform(post("/api/presences/manuelle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.etudiantId").value(1))
                .andExpect(jsonPath("$.source").value("FORMATEUR"));
    }
}
