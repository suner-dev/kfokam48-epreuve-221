package com.kfokam48.presencerelecture.relecture.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * B6, règle de gestion RG20 et exigence EF17 : « la note retenue est la moyenne des deux, et
 * elle est provisoire tant que les deux pairs n'ont pas tous deux rendu ».
 *
 * <p>La règle est ici exprimée une seule fois, sans Spring et sans base : c'est cette même
 * méthode qui alimente la réponse à l'auteur et celle au formateur, elles ne peuvent donc pas
 * diverger.
 */
class ReviewSummaryTest {

    @Test
    void sansAucuneNoteRendueLaNoteRetenueEstAbsenteEtProvisoire() {
        ReviewSummary resume = ReviewSummary.of(List.of());

        assertThat(resume.nombreDeNotes()).isZero();
        assertThat(resume.moyenne()).isNull();
        assertThat(resume.provisoire()).isTrue();
    }

    @Test
    void uneSeuleNoteRendueEstElleMemeMaisProvisoire() {
        ReviewSummary resume = ReviewSummary.of(List.of(15));

        assertThat(resume.nombreDeNotes()).isEqualTo(1);
        assertThat(resume.moyenne()).isEqualTo(15.0);
        assertThat(resume.provisoire())
                .as("la demande client : « on affiche sa note en attendant, mais marquée comme provisoire »")
                .isTrue();
    }

    @Test
    void deuxNotesRendentLaMoyenneNestPlusProvisoire() {
        ReviewSummary resume = ReviewSummary.of(List.of(12, 16));

        assertThat(resume.nombreDeNotes()).isEqualTo(2);
        assertThat(resume.moyenne()).isEqualTo(14.0);
        assertThat(resume.provisoire()).isFalse();
    }

    @Test
    void laMoyennePeutEtreFractionnaireLesNotesRestentEntieres() {
        // Q9 : chaque note est un entier 0-20. C'est leur moyenne qui ne l'est pas.
        ReviewSummary resume = ReviewSummary.of(List.of(15, 16));

        assertThat(resume.moyenne()).isEqualTo(15.5);
    }

    @Test
    void lesBornesDeLaNoteSontRespecteesAvantMoyenne() {
        assertThat(ReviewSummary.of(List.of(0, 20)).moyenne()).isEqualTo(10.0);
    }
}
