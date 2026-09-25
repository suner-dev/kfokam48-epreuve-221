package com.kfokam48.presencerelecture.session.domain;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_cours")
public class SessionCours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titre;
    private String code;
    private Long promotionId;
    private Long formateurId;
    private Instant ouvertureAt;
    private Instant expirationAt;
    private Instant finAt;
    private Instant clotureAt;

    protected SessionCours() {
    }

    public SessionCours(String titre, String code, Long promotionId, Instant ouvertureAt, Instant expirationAt) {
        this.titre = titre;
        this.code = code;
        this.promotionId = promotionId;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = expirationAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getCode() {
        return code;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public Long getFormateurId() {
        return formateurId;
    }

    public Instant getOuvertureAt() {
        return ouvertureAt;
    }

    public Instant getExpirationAt() {
        return expirationAt;
    }

    public Instant getFinAt() {
        return finAt;
    }

    public Instant getClotureAt() {
        return clotureAt;
    }

    public void refreshWindow(Instant ouvertureAt, Instant expirationAt) {
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = expirationAt;
        this.finAt = null;
        this.clotureAt = null;
    }

    public void finish(Instant finAt) {
        this.finAt = finAt;
    }

    public void close(Instant finAt, Instant clotureAt) {
        this.finAt = this.finAt == null ? finAt : this.finAt;
        this.clotureAt = clotureAt;
    }
}
