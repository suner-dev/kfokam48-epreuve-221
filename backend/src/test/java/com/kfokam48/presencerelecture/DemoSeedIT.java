package com.kfokam48.presencerelecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DemoSeedIT {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void chargeUnJeuDeDemonstrationRealiste() {
        assertThat(count("etudiant")).isGreaterThanOrEqualTo(60);
        assertThat(count("session_cours")).isGreaterThanOrEqualTo(2);
        assertThat(count("presence where source = 'ETUDIANT'")).isGreaterThan(0);
        assertThat(count("presence where source = 'FORMATEUR'")).isGreaterThan(0);
        assertThat(count("exercice where statut = 'RELU'")).isGreaterThan(0);
        assertThat(count("exercice where statut = 'EN_ATTENTE_DE_RELECTURE'")).isGreaterThan(0);
        assertThat(count("exercice where statut = 'EN_ATTENTE_SANS_RELECTEUR'")).isGreaterThan(0);
    }

    private long count(String tableAndPredicate) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableAndPredicate, Long.class);
    }
}
