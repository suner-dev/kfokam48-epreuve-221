package com.kfokam48.presencerelecture.relecture.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "relecture")
public class Relecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long exerciceId;
    private Long relecteurId;
    private Integer note;
    private String commentaire;
    private Instant commenceeAt;
    private Instant rendueAt;

    protected Relecture() {
    }

    public Relecture(Long exerciceId, Long relecteurId) {
        this.exerciceId = exerciceId;
        this.relecteurId = relecteurId;
    }

    public Long getId() {
        return id;
    }

    public Long getExerciceId() {
        return exerciceId;
    }

    public Long getRelecteurId() {
        return relecteurId;
    }

    public void assign(Long relecteurId) {
        this.relecteurId = relecteurId;
    }
}
