package com.kfokam48.presencerelecture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
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
class ExerciseControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deposeUnExerciceEtRefuseUnDoublon() throws Exception {
        String sessionBody = objectMapper.writeValueAsString(new CreateSessionRequest("Session exercice", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();
        String body = objectMapper.writeValueAsString(new CreateExerciseRequest(
                sessionId, 1L, "https://example.test/exercice/1"
        ));

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_SANS_RELECTEUR"));

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    void refuseUnLienInvalide() throws Exception {
        String sessionBody = objectMapper.writeValueAsString(new CreateSessionRequest("Session URI", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();
        String body = objectMapper.writeValueAsString(new CreateExerciseRequest(sessionId, 1L, "pas-une-uri"));

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }
}
