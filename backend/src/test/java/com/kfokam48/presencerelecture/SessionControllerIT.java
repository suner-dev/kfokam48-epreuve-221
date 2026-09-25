package com.kfokam48.presencerelecture;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import com.kfokam48.presencerelecture.session.api.CreatedSessionResponse;
import org.junit.jupiter.api.Assertions;
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
class SessionControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposeLeDetailDuneSession() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateSessionRequest("Session détail", 1L));
        String created = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.titre").value("Session détail"))
                .andExpect(jsonPath("$.promotionId").value(1))
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.ouvertureAt").isString())
                .andExpect(jsonPath("$.expirationAt").isString())
                .andExpect(jsonPath("$.finAt").value(nullValue()))
                .andExpect(jsonPath("$.clotureAt").value(nullValue()));
    }

    @Test
    void ouvreUneSessionAvecExpirationALaQuinzeMinutes() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateSessionRequest("Session Java", 1L));

        String response = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.ouvertureAt").isString())
                .andExpect(jsonPath("$.expirationAt").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CreatedSessionResponse created = objectMapper.readValue(response, CreatedSessionResponse.class);
        long seconds = created.expirationAt().getEpochSecond()
                - created.ouvertureAt().getEpochSecond();
        Assertions.assertEquals(900, seconds);
    }
}
