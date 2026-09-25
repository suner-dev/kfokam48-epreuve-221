package com.kfokam48.presencerelecture;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.relecture.api.SubmitReviewRequest;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
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
class ReviewControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RelectureRepository relectureRepository;

    @Test
    void listeLesRelecturesSansAuteurEtDemarreLaRelecture() throws Exception {
        SessionInfo session = createSession("Ecran relecteur");
        markPresence(session, 2L);
        long reviewId = createReview(session.id(), "https://example.test/exercice/relecteur");

        String taskPath = "$[?(@.relectureId == " + reviewId + ")]";
        mockMvc.perform(get("/api/relectures/a-faire").param("etudiantId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(taskPath + ".relectureId").value(hasItem((int) reviewId)))
                .andExpect(jsonPath(taskPath + ".exerciceId").isNotEmpty())
                .andExpect(jsonPath(taskPath + ".lienExercice").value(hasItem("https://example.test/exercice/relecteur")))
                .andExpect(jsonPath(taskPath + ".commenceeAt").value(hasItem(nullValue())))
                .andExpect(jsonPath(taskPath + ".etudiantId").doesNotExist());

        mockMvc.perform(post("/api/relectures/{id}/debut", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.commenceeAt").isString());

        mockMvc.perform(post("/api/relectures/{id}/debut", reviewId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_COMMENCEE"));
    }

    @Test
    void refuseUnEtudiantInconnuPourLaListe() throws Exception {
        mockMvc.perform(get("/api/relectures/a-faire").param("etudiantId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"));
    }

    @Test
    void rendUneNoteEtChangeLeStatut() throws Exception {
        SessionInfo session = createSession("Rendu note");
        markPresence(session, 2L);
        long reviewId = createReview(session.id(), "https://example.test/exercice/note");

        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitReviewRequest(new BigDecimal("17"), "Bon travail"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.note").value(17))
                .andExpect(jsonPath("$.commentaire").value("Bon travail"));

        mockMvc.perform(get("/api/sessions/{id}/exercices", session.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("RELU"));
    }

    @Test
    void corrigeUneRelectureAvantLaCloturePuisLaRefuseApres() throws Exception {
        SessionInfo session = createSession("Correction note");
        markPresence(session, 2L);
        long reviewId = createReview(session.id(), "https://example.test/exercice/correction");

        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitReviewRequest(new BigDecimal("17"), "Première version"))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitReviewRequest(new BigDecimal("18"), "Version corrigée"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.note").value(18))
                .andExpect(jsonPath("$.commentaire").value("Version corrigée"));

        mockMvc.perform(post("/api/sessions/{id}/cloture", session.id()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitReviewRequest(new BigDecimal("19"), "Trop tard"))))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    @Test
    void refuseUnSecondRendu() throws Exception {
        SessionInfo session = createSession("Doublon rendu");
        markPresence(session, 2L);
        long reviewId = createReview(session.id(), "https://example.test/exercice/doublon");
        String body = objectMapper.writeValueAsString(new SubmitReviewRequest(new BigDecimal("12"), "Commentaire"));

        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));
    }

    @Test
    void refuseUneNoteHorsBornesOuNonEntiere() throws Exception {
        SessionInfo session = createSession("Note invalide");
        markPresence(session, 2L);
        long reviewId = createReview(session.id(), "https://example.test/exercice/note-invalide");

        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":21,\"commentaire\":\"Trop\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":10.5,\"commentaire\":\"Décimal\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
    }

    @Test
    void refuseLautoRelecture() throws Exception {
        SessionInfo session = createSession("Auto relecture");
        long reviewId = createReview(session.id(), "https://example.test/exercice/auto");
        Relecture review = relectureRepository.findById(reviewId).orElseThrow();
        review.assign(1L);
        relectureRepository.save(review);

        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitReviewRequest(new BigDecimal("15"), "Interdit"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    private SessionInfo createSession(String title) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateSessionRequest(title, 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new SessionInfo(
                objectMapper.readTree(created).get("id").asLong(),
                objectMapper.readTree(created).get("code").asText()
        );
    }

    private void markPresence(SessionInfo session, long studentId) throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPresenceRequest(session.code(), studentId))))
                .andExpect(status().isCreated());
    }

    private long createReview(long sessionId, String link) throws Exception {
        String created = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExerciseRequest(sessionId, 1L, link))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String details = mockMvc.perform(get("/api/sessions/{id}/exercices", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(details).get(0).get("relectureId").asLong();
    }

    private record SessionInfo(long id, String code) {
    }
}
