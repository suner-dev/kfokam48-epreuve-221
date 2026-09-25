package com.kfokam48.presencerelecture.etudiant.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "etudiant")
public class Etudiant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String prenom;
    private Long promotionId;

    protected Etudiant() {
    }

    public Etudiant(String nom, String prenom, Long promotionId) {
        this.nom = nom;
        this.prenom = prenom;
        this.promotionId = promotionId;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public Long getPromotionId() {
        return promotionId;
    }
}
