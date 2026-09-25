package com.kfokam48.presencerelecture.presence.domain;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "code_tentative")
public class CodeTentative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long etudiantId;
    private int tentatives;
    private Instant bloqueJusqua;

    protected CodeTentative() {
    }

    public CodeTentative(Long etudiantId) {
        this.etudiantId = etudiantId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public int getTentatives() {
        return tentatives;
    }

    public Instant getBloqueJusqua() {
        return bloqueJusqua;
    }

    public void registerFailure(Instant maintenant, int maximum, int dureeSecondes) {
        tentatives++;
        if (tentatives >= maximum) {
            bloqueJusqua = maintenant.plusSeconds(dureeSecondes);
        }
    }

    public void reset() {
        tentatives = 0;
        bloqueJusqua = null;
    }
}
