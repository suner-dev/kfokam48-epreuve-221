package com.kfokam48.presencerelecture.exercice.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercice")
public class Exercice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sessionId;
    private Long etudiantId;
    private String lien;
    @Enumerated(EnumType.STRING)
    private StatutExercice statut;
    private Instant deposeAt;

    protected Exercice() {
    }

    public Exercice(Long sessionId, Long etudiantId, String lien, StatutExercice statut, Instant deposeAt) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.lien = lien;
        this.statut = statut;
        this.deposeAt = deposeAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public String getLien() {
        return lien;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public Instant getDeposeAt() {
        return deposeAt;
    }

    public void replaceLien(String lien) {
        this.lien = lien;
    }

    public void lockForCloture() {
        if (statut == StatutExercice.RELU) {
            statut = StatutExercice.RELU_VERROUILLE;
        } else if (statut != StatutExercice.RELU_VERROUILLE
                && statut != StatutExercice.EN_ATTENTE_VERROUILLE) {
            statut = StatutExercice.EN_ATTENTE_VERROUILLE;
        }
    }

    public void setStatut(StatutExercice statut) {
        this.statut = statut;
    }
}
