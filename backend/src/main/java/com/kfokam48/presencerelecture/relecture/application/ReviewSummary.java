package com.kfokam48.presencerelecture.relecture.application;

import java.util.List;

/**
 * Note retenue d'un exercice (EF17, RG20) : la moyenne des notes rendues par les pairs,
 * et le caractère provisoire de cette moyenne.
 *
 * <p>Une seule définition de la règle, partagée par le service qui répond à l'auteur et par
 * celui qui répond au formateur : les deux ne peuvent pas diverger.
 */
public record ReviewSummary(Integer nombreDeNotes, Double moyenne, boolean provisoire) {

    public static final int NOMBRE_DE_PAIRS_ATTENDUS = 2;

    public static ReviewSummary of(List<Integer> notes) {
        if (notes.isEmpty()) {
            return new ReviewSummary(0, null, true);
        }
        double somme = 0;
        for (Integer note : notes) {
            somme += note;
        }
        return new ReviewSummary(notes.size(), somme / notes.size(), notes.size() < NOMBRE_DE_PAIRS_ATTENDUS);
    }
}
