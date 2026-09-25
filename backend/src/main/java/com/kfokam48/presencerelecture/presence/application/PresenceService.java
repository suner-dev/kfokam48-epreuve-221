package com.kfokam48.presencerelecture.presence.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.etudiant.application.EtudiantService;
import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.exercice.application.ExerciseService;
import com.kfokam48.presencerelecture.presence.api.ManualPresenceRequest;
import com.kfokam48.presencerelecture.presence.api.MarkPresenceRequest;
import com.kfokam48.presencerelecture.presence.api.PresenceResponse;
import com.kfokam48.presencerelecture.presence.api.PresenceSessionResponse;
import com.kfokam48.presencerelecture.presence.domain.Presence;
import com.kfokam48.presencerelecture.presence.domain.PresenceRepository;
import com.kfokam48.presencerelecture.presence.domain.SourcePresence;
import com.kfokam48.presencerelecture.session.application.SessionService;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PresenceService {
    private final PresenceRepository repository;
    private final ExerciseService exerciseService;
    private final SessionService sessionService;
    private final EtudiantService etudiantService;
    private final Clock clock;

    public PresenceService(
            PresenceRepository repository,
            ExerciseService exerciseService,
            SessionService sessionService,
            EtudiantService etudiantService,
            Clock clock
    ) {
        this.repository = repository;
        this.exerciseService = exerciseService;
        this.sessionService = sessionService;
        this.etudiantService = etudiantService;
        this.clock = clock;
    }

    public PresenceResponse mark(MarkPresenceRequest request) {
        SessionCours session = sessionService.requireByCode(request.code());
        checkUsable(session);
        Etudiant etudiant = etudiantService.require(request.etudiantId(), session.getPromotionId());
        if (repository.findBySessionIdAndEtudiantId(session.getId(), etudiant.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "DEJA_PRESENT", "La présence est déjà enregistrée pour cette session.");
        }
        Presence presence = repository.save(new Presence(
                session.getId(), etudiant.getId(), SourcePresence.ETUDIANT, clock.instant()
        ));
        exerciseService.assignPending(session.getId());
        return new PresenceResponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(), presence.getSource());
    }

    public PresenceResponse addManually(ManualPresenceRequest request) {
        SessionCours session = sessionService.require(request.sessionId());
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        Etudiant etudiant = etudiantService.require(request.etudiantId(), session.getPromotionId());
        if (repository.findBySessionIdAndEtudiantId(session.getId(), etudiant.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "DEJA_PRESENT", "La présence est déjà enregistrée pour cette session.");
        }
        Presence presence = repository.save(new Presence(
                session.getId(), etudiant.getId(), SourcePresence.FORMATEUR, clock.instant()
        ));
        exerciseService.assignPending(session.getId());
        return new PresenceResponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(), presence.getSource());
    }

    @Transactional(readOnly = true)
    public List<PresenceSessionResponse> findBySession(Long sessionId) {
        SessionCours session = sessionService.require(sessionId);
        Map<Long, Presence> presenceParEtudiant = repository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(Presence::getEtudiantId, Function.identity()));
        return etudiantService.findByPromotion(session.getPromotionId()).stream()
                .map(etudiant -> {
                    Presence presence = presenceParEtudiant.get(etudiant.getId());
                    return new PresenceSessionResponse(
                            etudiant.getId(), etudiant.getNom(), presence != null,
                            presence == null ? null : presence.getSource(),
                            presence == null ? null : presence.getMarqueeAt()
                    );
                })
                .sorted((left, right) -> left.nom().compareTo(right.nom()))
                .toList();
    }

    private void checkUsable(SessionCours session) {
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        if (!clock.instant().isBefore(session.getExpirationAt())) {
            throw new ApiException(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré.");
        }
        if (session.getFinAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_TERMINEE", "La session est terminée.");
        }
    }
}
