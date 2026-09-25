package com.kfokam48.presencerelecture;

import com.fasterxml.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void agregeLeTableauEtCalculeLaMoyenne() throws Exception {
        JsonNode before = dashboard();
        long reviewerPresencesBefore = row(before, 1L).get("presences").asLong();
        long reviewerPendingBefore = row(before, 1L).get("relecturesEnAttente").asLong();
        long authorExercisesBefore = row(before, 3L).get("exercicesDeposes").asLong();

        SessionInfo session = createSession();
        markPresence(session, 1L);
        createExercise(session.id(), 3L, "https://example.test/exercice/tableau");
        long reviewId = reviewId(session.id());
        render(reviewId, 15, "Premier");
        createExercise(session.id(), 2L, "https://example.test/exercice/tableau-pending");

        JsonNode after = dashboard();
        mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk());
        assertThat(row(after, 1L).get("presences").asLong()).isEqualTo(reviewerPresencesBefore + 1);
        assertThat(row(after, 1L).get("relecturesEnAttente").asLong()).isEqualTo(reviewerPendingBefore + 1);
        assertThat(row(after, 3L).get("exercicesDeposes").asLong()).isEqualTo(authorExercisesBefore + 1);
        assertThat(row(after, 3L).get("moyenne").asDouble()).isEqualTo(15.0);
    }

    @Test
    void refuseUnePromotionInconnue() throws Exception {
        mockMvc.perform(get("/api/tableau").param("promotionId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    private JsonNode dashboard() throws Exception {
        String response = mockMvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode row(JsonNode rows, long studentId) {
        for (JsonNode row : rows) {
            if (row.get("etudiantId").asLong() == studentId) {
                return row;
            }
        }
        throw new AssertionError("Étudiant absent du tableau : " + studentId);
    }

    private SessionInfo createSession() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateSessionRequest("Tableau", 1L));
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

    private long createExercise(long sessionId, long studentId, String link) throws Exception {
        String created = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExerciseRequest(sessionId, studentId, link))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    private long reviewId(long sessionId) throws Exception {
        String details = mockMvc.perform(get("/api/sessions/{id}/exercices", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var reviews = objectMapper.readTree(details);
        return reviews.get(reviews.size() - 1).get("relectureId").asLong();
    }

    private void render(long reviewId, int note, String commentaire) throws Exception {
        mockMvc.perform(post("/api/relectures/{id}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":" + note + ",\"commentaire\":\"" + commentaire + "\"}"))
                .andExpect(status().isOk());
    }


    private record SessionInfo(long id, String code) {
    }
}
