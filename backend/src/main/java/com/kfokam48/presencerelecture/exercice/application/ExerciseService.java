package com.kfokam48.presencerelecture.exercice.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.exercice.api.CreateExerciseRequest;
import com.kfokam48.presencerelecture.exercice.api.ExerciseCreatedResponse;
import com.kfokam48.presencerelecture.exercice.api.ExerciseDetailResponse;
import com.kfokam48.presencerelecture.exercice.api.ReplaceExerciseRequest;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.exercice.domain.StatutExercice;
import com.kfokam48.presencerelecture.presence.domain.Presence;
import com.kfokam48.presencerelecture.presence.domain.PresenceRepository;
import com.kfokam48.presencerelecture.relecture.api.ReviewAssignmentResponse;
import com.kfokam48.presencerelecture.relecture.application.ReviewSummary;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.net.URI;
import java.util.ArrayList;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExerciseService {
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;
    private final PresenceRepository presenceRepository;
    private final SessionService sessionService;
    private final EtudiantService etudiantService;
    private final Clock clock;

    public ExerciseService(
            ExerciceRepository exerciceRepository,
            RelectureRepository relectureRepository,
            PresenceRepository presenceRepository,
            SessionService sessionService,
            EtudiantService etudiantService,
            Clock clock
    ) {
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
        this.presenceRepository = presenceRepository;
        this.sessionService = sessionService;
        this.etudiantService = etudiantService;
        this.clock = clock;
    }

    public ExerciseCreatedResponse create(CreateExerciseRequest request) {
        validateUri(request.lien());
        SessionCours session = sessionService.require(request.sessionId(), HttpStatus.BAD_REQUEST);
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        Etudiant author = etudiantService.require(request.etudiantId(), session.getPromotionId());
        if (exerciceRepository.findBySessionIdAndEtudiantId(session.getId(), author.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE", "Un exercice est déjà déposé pour cette session.");
        }
        boolean eligiblePresent = !eligibleReviewers(session.getId(), author.getId()).isEmpty();
        StatutExercice statut = eligiblePresent
                ? StatutExercice.EN_ATTENTE_DE_RELECTURE
                : StatutExercice.EN_ATTENTE_SANS_RELECTEUR;
        Exercice exercise = exerciceRepository.save(new Exercice(
                session.getId(), author.getId(), request.lien(), statut, clock.instant()
        ));
        // RG20 : deux affectations au plus. Une seule est créée ici, l'autre est tirée par
        // assignPending si un second pair est éligible ; l'affectation vide du trou H3 reste possible.
        relectureRepository.save(new Relecture(exercise.getId(), null));
        assignPending(session.getId());
        return new ExerciseCreatedResponse(exercise.getId(), exercise.getStatut());
    }

    public ExerciseCreatedResponse replace(Long id, ReplaceExerciseRequest request) {
        validateUri(request.lien());
        Exercice exercise = exerciceRepository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "EXERCICE_INCONNU",
                "L'exercice demandé n'existe pas."
        ));
        SessionCours session = sessionService.require(exercise.getSessionId());
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        boolean commencee = relectureRepository.findByExerciceIdOrderById(id).stream()
                .anyMatch(review -> review.getCommenceeAt() != null);
        if (commencee) {
            throw new ApiException(HttpStatus.CONFLICT, "RELECTURE_COMMENCEE", "La relecture a commencé.");
        }
        exercise.replaceLien(request.lien());
        return new ExerciseCreatedResponse(exercise.getId(), exercise.getStatut());
    }

    @Transactional(readOnly = true)
    public List<ExerciseDetailResponse> details(Long sessionId) {
        sessionService.require(sessionId);
        return exerciceRepository.findBySessionIdOrderById(sessionId).stream()
                .map(exercise -> {
                    List<Relecture> reviews = relectureRepository.findByExerciceIdOrderById(exercise.getId());
                    ReviewSummary resume = ReviewSummary.of(reviews.stream()
                            .filter(review -> review.getRendueAt() != null)
                            .map(Relecture::getNote)
                            .toList());
                    return ExerciseDetailResponse.of(
                            exercise.getId(),
                            exercise.getEtudiantId(),
                            exercise.getStatut(),
                            reviews.stream()
                                    .map(review -> new ReviewAssignmentResponse(
                                            review.getId(),
                                            review.getRelecteurId(),
                                            review.getCommenceeAt(),
                                            review.getRendueAt()))
                                    .toList(),
                            resume.moyenne(),
                            resume.provisoire()
                    );
                })
                .toList();
    }

    /**
     * Enveloppe étape 3 (RG20) : jusqu'à deux affectations par exercice, deux pairs distincts,
     * jamais l'auteur (Q5/RG5). L'affectation vide du trou H3 est Completed progressively —
     * une nouvelle présence peut donc compléter un exercice à un seul pair, jusqu'à deux.
     */
    public void assignPending(Long sessionId) {
        for (Exercice exercise : exerciceRepository.findBySessionIdOrderById(sessionId)) {
            List<Relecture> reviews = relectureRepository.findByExerciceIdOrderById(exercise.getId());
            if (reviews.isEmpty()) {
                reviews = List.of(relectureRepository.save(new Relecture(exercise.getId(), null)));
            }
            List<Long> dejaAssignes = reviews.stream()
                    .map(Relecture::getRelecteurId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            int restantes = ReviewSummary.NOMBRE_DE_PAIRS_ATTENDUS - dejaAssignes.size();
            if (restantes <= 0) {
                continue;
            }
            // Liste mutable : le tirage retire le pair choisi de la liste, sans quoi deux
            // affectations pourraient revenir au même pair.
            List<Long> candidats = eligibleReviewers(sessionId, exercise.getEtudiantId()).stream()
                    .filter(candidat -> !dejaAssignes.contains(candidat))
                    .collect(Collectors.toCollection(ArrayList::new));
            // Le nombre de tirages est fige avant la boucle : `candidats` rétrécit à chaque
            // tirage, donc tester `tirage < candidats.size()` ne tirerait qu'un seul pair.
            int aTirer = Math.min(restantes, candidats.size());
            for (int tirage = 0; tirage < aTirer; tirage++) {
                Long reviewerId = candidats.remove(
                        ThreadLocalRandom.current().nextInt(candidats.size())
                );
                Relecture vide = reviews.stream()
                        .filter(review -> review.getRelecteurId() == null)
                        .findFirst()
                        .orElse(null);
                if (vide == null) {
                    vide = relectureRepository.save(new Relecture(exercise.getId(), reviewerId));
                } else {
                    vide.assign(reviewerId);
                }
                exercise.setStatut(StatutExercice.EN_ATTENTE_DE_RELECTURE);
            }
        }
    }

    private List<Long> eligibleReviewers(Long sessionId, Long authorId) {
        return presenceRepository.findBySessionId(sessionId).stream()
                .map(Presence::getEtudiantId)
                .filter(id -> !id.equals(authorId))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void validateUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getScheme() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE", "Le lien doit être une URI valide.");
        }
    }
}
