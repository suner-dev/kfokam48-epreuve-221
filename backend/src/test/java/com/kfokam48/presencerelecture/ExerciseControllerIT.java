package com.kfokam48.presencerelecture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.exercice.api.ReplaceExerciseRequest;
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
    void affecteUnRelecteurPresentEtReaffecteUnExerciceEnAttente() throws Exception {
        String sessionBody = objectMapper.writeValueAsString(new CreateSessionRequest("Session affectation", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();
        String code = objectMapper.readTree(created).get("code").asText();

        String exerciseBody = objectMapper.writeValueAsString(new CreateExerciseRequest(
                sessionId, 1L, "https://example.test/exercice/affectation"
        ));
        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exerciseBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_SANS_RELECTEUR"));

        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPresenceRequest(code, 2L))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/sessions/{id}/exercices", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relecteurId").value(2))
                .andExpect(jsonPath("$[0].relectureId").isNumber());
    }

    @Test
    void remplaceLeLienAvantLeDemarrageDeLaRelecture() throws Exception {
        String sessionBody = objectMapper.writeValueAsString(new CreateSessionRequest("Session remplacement", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();
        String code = objectMapper.readTree(created).get("code").asText();
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPresenceRequest(code, 2L))))
                .andExpect(status().isCreated());
        String exercise = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExerciseRequest(
                                sessionId, 1L, "https://example.test/exercice/avant"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long exerciseId = objectMapper.readTree(exercise).get("id").asLong();
        String details = mockMvc.perform(get("/api/sessions/{id}/exercices", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long reviewId = objectMapper.readTree(details).get(0).get("relectureId").asLong();

        mockMvc.perform(put("/api/exercices/{id}", exerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplaceExerciseRequest(
                                "https://example.test/exercice/apres"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciseId));

        mockMvc.perform(post("/api/relectures/{id}/debut", reviewId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/exercices/{id}", exerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReplaceExerciseRequest(
                                "https://example.test/exercice/interdit"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_COMMENCEE"));
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
