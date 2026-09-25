package com.kfokam48.presencerelecture.relecture.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.domain.EtudiantRepository;
import com.kfokam48.presencerelecture.relecture.api.ReceivedReviewResponse;
import com.kfokam48.presencerelecture.exercice.domain.Exercice;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReceivedReviewService {
    private final RelectureRepository relectureRepository;
    private final EtudiantRepository etudiantRepository;

    public ReceivedReviewService(
            RelectureRepository relectureRepository,
            EtudiantRepository etudiantRepository
    ) {
        this.relectureRepository = relectureRepository;
        this.etudiantRepository = etudiantRepository;
    }

    public List<ReceivedReviewResponse> findForAuthor(Long authorId) {
        if (!etudiantRepository.existsById(authorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "L'étudiant demandé n'existe pas.");
        }
        return relectureRepository.findReviewedExercisesForAuthor(authorId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Une entrée par exercice, jamais par affectation : deux relectures sur le même exercice
     * donnent une seule note retenue, celle que l'auteur lit (EF17).
     */
    private ReceivedReviewResponse toResponse(Exercice exercise) {
        List<Relecture> rendues = relectureRepository.findRenderedByExercise(exercise.getId());
        ReviewSummary resume = ReviewSummary.of(rendues.stream().map(Relecture::getNote).toList());
        String commentaires = rendues.stream()
                .map(Relecture::getCommentaire)
                .filter(commentaire -> commentaire != null && !commentaire.isBlank())
                .collect(Collectors.joining(" — "));
        return new ReceivedReviewResponse(
                exercise.getId(),
                exercise.getSessionId(),
                exercise.getLien(),
                resume.moyenne(),
                commentaires,
                resume.nombreDeNotes(),
                resume.provisoire()
        );
    }
}
