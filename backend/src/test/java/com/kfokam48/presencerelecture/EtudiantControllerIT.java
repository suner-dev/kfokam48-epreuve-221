package com.kfokam48.presencerelecture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EtudiantControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listeLesEtudiantsDeLaPromotionDemo() throws Exception {
        mockMvc.perform(get("/api/etudiants").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].nom").isString())
                .andExpect(jsonPath("$[0].prenom").isString());
    }

    @Test
    void refuseUnePromotionInconnue() throws Exception {
        mockMvc.perform(get("/api/etudiants").param("promotionId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    void masqueLidentiteDuRelecteurDansLesResultatsRecus() throws Exception {
        String session = objectMapper.writeValueAsString(new CreateSessionRequest("Anonymat", 1L));
        String createdSession = mockMvc.perform(post("/api/sessions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(session))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(createdSession).get("id").asLong();
        String code = objectMapper.readTree(createdSession).get("code").asText();

        mockMvc.perform(post("/api/presences")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPresenceRequest(code, 2L))))
                .andExpect(status().isCreated());

        String createdExercise = mockMvc.perform(post("/api/exercices")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExerciseRequest(
                                sessionId, 1L, "https://example.test/exercice/anonymat"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long exerciseId = objectMapper.readTree(createdExercise).get("id").asLong();
        String details = mockMvc.perform(get("/api/sessions/{id}/exercices", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long reviewId = objectMapper.readTree(details).get(0).get("relectureId").asLong();

        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"note\":15,\"commentaire\":\"Anonyme\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/etudiants/{id}/relectures-recues", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exerciceId").value(exerciseId))
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].lienExercice").value("https://example.test/exercice/anonymat"))
                .andExpect(jsonPath("$[0].note").value(15))
                .andExpect(jsonPath("$[0].commentaire").value("Anonyme"))
                .andExpect(jsonPath("$[0].relecteurId").doesNotExist())
                .andExpect(jsonPath("$[0].relecteur").doesNotExist());
    }

    @Test
    void refuseUnEtudiantInconnu() throws Exception {
        mockMvc.perform(get("/api/etudiants/{id}/relectures-recues", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }
}
