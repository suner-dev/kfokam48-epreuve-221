package com.kfokam48.presencerelecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Enveloppe étape 3 — issues #70 et #71.
 *
 * <p>Trois preuves de bout en bout du changement de besoin : deux pairs distincts sont affectés,
 * la note retenue est leur moyenne, et elle est marquée provisoire tant qu'un seul a rendu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeuxRelecteursIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void affecteDeuxPairsDistinctsJamaisLauteur() throws Exception {
        long sessionId = ouvrirSession("Deux relecteurs");
        String code = codeDe(sessionId);
        marquer(code, 5L);
        marquer(code, 6L);
        marquer(code, 7L);

        long exerciceId = deposer(sessionId, 1L);

        JsonPathNode detail = exercice(sessionId);
        assertThat(detail.relecteurs).as("deux pairs sont affectes (RG20)").hasSize(2);
        assertThat(detail.relecteurs.get(0).relecteurId)
                .as("le premier pair ne peut pas etre l'auteur (Q5/RG5)")
                .isNotEqualTo(1L);
        assertThat(detail.relecteurs.get(0).relecteurId)
                .isNotEqualTo(detail.relecteurs.get(1).relecteurId);
        assertThat(detail.provisoire).as("aucune note rendue : provisoire").isTrue();
        assertThat(detail.noteRetenue).as("aucune note rendue : pas de note").isNull();
        assertThat(exerciceId).isPositive();
    }

    @Test
    void laNoteRetenueEstLaMoyenneDesDeuxEtNestPlusProvisoire() throws Exception {
        long sessionId = ouvrirSession("Moyenne des deux");
        String code = codeDe(sessionId);
        marquer(code, 5L);
        marquer(code, 6L);

        deposer(sessionId, 1L);
        JsonPathNode detail = exercice(sessionId);
        assertThat(detail.relecteurs).hasSize(2);

        rendre(detail.relecteurs.get(0).relectureId, 12, "Première relecture.");
        rendre(detail.relecteurs.get(1).relectureId, 16, "Seconde relecture.");

        String body = mockMvc.perform(get("/api/etudiants/{id}/relectures-recues", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].note").value(14.0))
                .andExpect(jsonPath("$[0].nbNotes").value(2))
                .andExpect(jsonPath("$[0].provisoire").value(false))
                .andExpect(jsonPath("$[0].commentaire").value("Première relecture. — Seconde relecture."))
                .andExpect(jsonPath("$[0].relecteurId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("relecteurId\":");
    }

    @Test
    void avecUnSeulPairDisponibleIlNExisteQuUneAffectation() throws Exception {
        long sessionId = ouvrirSession("Un seul pair");
        String code = codeDe(sessionId);
        marquer(code, 5L);

        deposer(sessionId, 1L);
        JsonPathNode detail = exercice(sessionId);

        assertThat(detail.relecteurs)
                .as("un seul candidat eligible : une seule affectation, jamais deux fois le meme pair")
                .hasSize(1);
        assertThat(detail.relecteurs.get(0).relecteurId).isEqualTo(5L);
    }

    // --- helpers -------------------------------------------------------------

    private record Affectation(long relectureId, Long relecteurId) {
    }

    private record JsonPathNode(List<Affectation> relecteurs, Double noteRetenue, boolean provisoire) {
    }

    private long ouvrirSession(String titre) throws Exception {
        String body = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSessionRequest(titre, 1L))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String codeDe(long sessionId) throws Exception {
        String body = mockMvc.perform(get("/api/sessions/{id}", sessionId))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("code").asText();
    }

    private void marquer(String code, Long etudiantId) throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPresenceRequest(code, etudiantId))))
                .andExpect(status().isCreated());
    }

    private long deposer(long sessionId, Long etudiantId) throws Exception {
        String body = mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExerciseRequest(
                                sessionId, etudiantId, "https://example.test/exercice/deux-pairs"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void rendre(long relectureId, int note, String commentaire) throws Exception {
        mockMvc.perform(post("/api/relectures/{id}", relectureId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":" + note + ",\"commentaire\":\"" + commentaire + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonPathNode exercice(long sessionId) throws Exception {
        String body = mockMvc.perform(get("/api/sessions/{id}/exercices", sessionId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode noeud = objectMapper.readTree(body).get(0);
        List<Affectation> affectations = new ArrayList<>();
        for (JsonNode affectation : noeud.get("relecteurs")) {
            affectations.add(new Affectation(
                    affectation.get("relectureId").asLong(),
                    affectation.get("relecteurId").isNull() ? null : affectation.get("relecteurId").asLong()
            ));
        }
        return new JsonPathNode(
                affectations,
                noeud.get("noteRetenue").isNull() ? null : noeud.get("noteRetenue").asDouble(),
                noeud.get("provisoire").asBoolean()
        );
    }
}
