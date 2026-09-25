package com.kfokam48.presencerelecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Issue #67 — « Deux étudiants côte à côte, ils ont tapé le code presque en même temps et il
 * n'y en a qu'un seul qui apparaît dans ma liste. »
 *
 * <p>Les marquages sont envoyés réellement en parallèle, avec un verrou de départ commun pour
 * qu'ils entrent en même temps. Un test séquentiel ne peut pas révéler ce défaut : il ne se
 * produit que sous concurrence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresenceConcurrenceIT {
    private static final List<Long> ETUDIANTS = List.of(4L, 5L, 6L, 7L, 8L, 9L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void conserveToutesLesPresencesMarqueesEnMemeTemps() throws Exception {
        String created = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSessionRequest("Session concurrente", 1L))))
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();
        String code = objectMapper.readTree(created).get("code").asText();

        List<Integer> statuts = marquerEnParallele(code);

        assertThat(statuts)
                .as("le contrat annonce 201 pour chaque marquage : aucun ne doit échouer sous concurrence")
                .containsOnly(201);

        assertThat(etudiantsPresents(sessionId))
                .as("le formateur doit voir tous les étudiants qui ont tapé le code en même temps")
                .containsExactlyInAnyOrderElementsOf(ETUDIANTS);
    }

    private List<Integer> marquerEnParallele(String code) throws Exception {
        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(ETUDIANTS.size());
        try {
            List<Callable<Integer>> appels = new ArrayList<>();
            for (Long etudiantId : ETUDIANTS) {
                appels.add(() -> {
                    depart.await(10, TimeUnit.SECONDS);
                    return marquerPresence(code, etudiantId);
                });
            }
            List<Future<Integer>> resultats = new ArrayList<>();
            for (Callable<Integer> appel : appels) {
                resultats.add(pool.submit(appel));
            }
            depart.countDown();

            List<Integer> statuts = new ArrayList<>();
            for (Future<Integer> resultat : resultats) {
                statuts.add(resultat.get(90, TimeUnit.SECONDS));
            }
            return statuts;
        } finally {
            pool.shutdownNow();
        }
    }

    private int marquerPresence(String code, Long etudiantId) {
        try {
            return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/presences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MarkPresenceRequest(code, etudiantId))))
                    .andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private List<Long> etudiantsPresents(long sessionId) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/sessions/{id}/presences", sessionId))
                .andReturn().getResponse().getContentAsString();
        List<Long> presents = new ArrayList<>();
        for (JsonNode row : objectMapper.readTree(body)) {
            if (row.get("present").asBoolean()) {
                presents.add(row.get("etudiantId").asLong());
            }
        }
        return presents;
    }
}
