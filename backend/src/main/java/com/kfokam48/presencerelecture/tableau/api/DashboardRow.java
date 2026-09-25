package com.kfokam48.presencerelecture.tableau.api;

public record DashboardRow(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        Double moyenne,
        long relecturesEnAttente
) {
}
