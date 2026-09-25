package com.kfokam48.presencerelecture.relecture.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.domain.EtudiantRepository;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.relecture.api.ReceivedReviewResponse;
import com.kfokam48.presencerelecture.relecture.domain.Relecture;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReceivedReviewService {
    private final RelectureRepository relectureRepository;
    private final ExerciceRepository exerciceRepository;
    private final EtudiantRepository etudiantRepository;

    public ReceivedReviewService(
            RelectureRepository relectureRepository,
            ExerciceRepository exerciceRepository,
            EtudiantRepository etudiantRepository
    ) {
        this.relectureRepository = relectureRepository;
        this.exerciceRepository = exerciceRepository;
        this.etudiantRepository = etudiantRepository;
    }

    public List<ReceivedReviewResponse> findForAuthor(Long authorId) {
        if (!etudiantRepository.existsById(authorId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "L'étudiant demandé n'existe pas.");
        }
        return relectureRepository.findRenderedForAuthor(authorId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ReceivedReviewResponse toResponse(Relecture review) {
        var exercise = exerciceRepository.findById(review.getExerciceId()).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "EXERCICE_INCONNUE",
                "L'exercice associé n'existe pas."
        ));
        return new ReceivedReviewResponse(
                exercise.getId(), exercise.getSessionId(), exercise.getLien(), review.getNote(), review.getCommentaire()
        );
    }
}
