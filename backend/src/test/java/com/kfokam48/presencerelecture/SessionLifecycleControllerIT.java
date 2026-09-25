package com.kfokam48.presencerelecture;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import java.time.Instant;
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
class SessionLifecycleControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExerciceRepository exerciceRepository;

    @Test
    void terminePuisClotureUneSession() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateSessionRequest("Session à clôturer", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/sessions/{id}/fin", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.finAt").isString());

        Exercice enAttente = exerciceRepository.save(new Exercice(
                id, 1L, "https://example.test/exercice/attente", StatutExercice.EN_ATTENTE_DE_RELECTURE, Instant.now()
        ));
        Exercice relu = exerciceRepository.save(new Exercice(
                id, 2L, "https://example.test/exercice/relu", StatutExercice.RELU, Instant.now()
        ));

        mockMvc.perform(post("/api/sessions/{id}/cloture", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.finAt").isString())
                .andExpect(jsonPath("$.clotureAt").isString());

        String pathAttente = "$[?(@.id == " + enAttente.getId() + ")]";
        String pathRelu = "$[?(@.id == " + relu.getId() + ")]";
        mockMvc.perform(get("/api/sessions/{id}/exercices", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(pathAttente + ".statut").value(hasItem("EN_ATTENTE_VERROUILLE")))
                .andExpect(jsonPath(pathRelu + ".statut").value(hasItem("RELU_VERROUILLE")));
    }
}
