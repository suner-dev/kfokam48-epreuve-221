package com.kfokam48.presencerelecture;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

/**
 * B4 + B2 : aucune réponse d'erreur ne sort du format {code,message} et tout code
 * renvoyé par le service est approuvé dans api/contrat.yaml (consignes 8 et 2.7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiErrorContractIT {
    private static final Pattern CODE_LITERAL = Pattern.compile("\"([A-Z][A-Z_]{4,})\"");
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void repond405AuFormatContractuelPourUneMethodeNonSupportee() throws Exception {
        mockMvc.perform(delete("/api/sessions"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("METHODE_NON_AUTORISEE"))
                .andExpect(jsonPath("$.message").value("Cette méthode HTTP n'est pas autorisée sur ce chemin."))
                .andExpect(jsonPath("$.*", hasSize(2)));
    }

    @Test
    void repond404JsonEtNonLaPageParDefautPourUneRouteInexistante() throws Exception {
        mockMvc.perform(get("/api/nexiste-pas"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_INTROUVABLE"))
                .andExpect(jsonPath("$.*", hasSize(2)));
    }

    @Test
    void repond400ContractuelPourUnCorpsJsonIllisible() throws Exception {
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.*", hasSize(2)));
    }

    @Test
    void repond400ChampManquantSansChampTechnique() throws Exception {
        // « code » absent : le format imposé reste exactement {code,message}, sans trace ni exception.
        mockMvc.perform(post("/api/presences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MarkPresenceRequest(null, 1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.*", hasSize(2)));
    }

    /**
     * Garde-fou B2/§8 : tout code d'erreur écrit en dur dans la couche applicative doit être
     * déclaré dans le contrat. Un code inventé casse donc le build au lieu de passer inaperçu.
     */
    @Test
    void toutCodeEmisEstApprouveDansLeContrat() throws IOException {
        String contract = readContract();
        SortedSet<String> undeclared = new TreeSet<>();
        try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
            sources.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    Matcher matcher = CODE_LITERAL.matcher(Files.readString(path));
                    while (matcher.find()) {
                        String code = matcher.group(1);
                        if (!contract.contains(code)) {
                            undeclared.add(path.getFileName() + " -> " + code);
                        }
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }
        org.assertj.core.api.Assertions.assertThat(undeclared)
                .as("codes d'erreur absents de api/contrat.yaml (consigne 8 : ne pas inventer un code)")
                .isEmpty();
    }

    /**
     * Consigne 9 : les cinq opérations imposées ne doivent voir aucun statut ajouté, aucun statut
     * supprimé. Seules les trois réponses additives approuvées à l'analyse (410 sur la dépose,
     * 404 et 410 sur la relecture) restent admises.
     */
    @Test
    @SuppressWarnings("unchecked")
    void lesOperationsImposeesNeDepassentPasLesStatutsApprouves() throws IOException {
        Map<String, Object> contract = (Map<String, Object>) new Yaml().load(readContract());
        Map<String, Object> paths = (Map<String, Object>) contract.get("paths");
        Map<String, SortedSet<String>> approuves = new TreeMap<>();
        approuves.put("POST /api/sessions", statuts("201", "400"));
        approuves.put("POST /api/presences", statuts("201", "400", "409", "410"));
        approuves.put("POST /api/exercices", statuts("201", "400", "409", "410"));
        approuves.put("POST /api/relectures/{id}", statuts("200", "400", "403", "404", "409", "410"));
        approuves.put("GET /api/tableau", statuts("200", "404"));

        Map<String, String> declares = new TreeMap<>();
        approuves.forEach((operation, attendu) -> {
            String[] decoupe = operation.split(" ", 2);
            Map<String, Object> operationContract = (Map<String, Object>) ((Map<String, Object>) paths.get(decoupe[1])).get(decoupe[0].toLowerCase());
            SortedSet<String> trouves = new TreeSet<>(((Map<String, Object>) operationContract.get("responses")).keySet());
            if (!trouves.equals(attendu)) {
                declares.put(operation, "attendus " + attendu + " mais déclarés " + trouves);
            }
        });
        org.assertj.core.api.Assertions.assertThat(declares)
                .as("statuts des cinq opérations imposées (consigne 9)")
                .isEmpty();
    }

    /**
     * Un identifiant de corps inconnu sur une écriture imposée reste du ressort de la 400 : le
     * contrat imposé ne déclare aucun 404 sur POST /api/sessions et POST /api/exercices.
     */
    @Test
    void lesIdentifiantsInconnusDuneEcritureImposeeRepondent400() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSessionRequest("Session promotion inconnue", 9999L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));

        mockMvc.perform(post("/api/exercices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExerciseRequest(9999L, 1L, "https://example.com/exercice"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION_INCONNUE"));
    }

    private static SortedSet<String> statuts(String... statuts) {
        return new TreeSet<>(Arrays.asList(statuts));
    }

    private String readContract() throws IOException {
        for (Path candidate : new Path[] {Path.of("api", "contrat.yaml"), Path.of("..", "api", "contrat.yaml")}) {
            if (Files.exists(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException("api/contrat.yaml introuvable depuis " + Path.of("").toAbsolutePath());
    }
}
