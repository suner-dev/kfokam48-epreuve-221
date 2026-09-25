package com.kfokam48.presencerelecture.session.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.promotion.application.PromotionService;
import com.kfokam48.presencerelecture.session.api.CloseSessionResponse;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import com.kfokam48.presencerelecture.session.api.CreatedSessionResponse;
import com.kfokam48.presencerelecture.session.api.SessionDetailResponse;
import com.kfokam48.presencerelecture.session.api.FinishSessionResponse;
import com.kfokam48.presencerelecture.session.domain.SessionCours;
import com.kfokam48.presencerelecture.session.domain.SessionCoursRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionService {
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SessionCoursRepository repository;
    private final PromotionService promotionService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SessionService(
            SessionCoursRepository repository,
            PromotionService promotionService,
            Clock clock
    ) {
        this.repository = repository;
        this.promotionService = promotionService;
        this.clock = clock;
    }

    public CreatedSessionResponse create(CreateSessionRequest request) {
        promotionService.require(request.promotionId(), HttpStatus.BAD_REQUEST);
        Instant ouvertureAt = clock.instant();
        SessionCours session = repository.save(new SessionCours(
                request.titre(),
                uniqueCode(),
                request.promotionId(),
                ouvertureAt,
                ouvertureAt.plus(15, ChronoUnit.MINUTES)
        ));
        return new CreatedSessionResponse(session.getId(), session.getCode(), session.getOuvertureAt(), session.getExpirationAt());
    }

    public SessionCours require(Long id) {
        return require(id, HttpStatus.NOT_FOUND);
    }

    /**
     * Même règle que pour la promotion : sur une écriture imposée (POST /api/exercices), le
     * sessionId provient du corps et le contrat n'y déclare pas de 404, donc 400.
     */
    public SessionCours require(Long id, HttpStatus statutAbsence) {
        return repository.findById(id).orElseThrow(() -> new ApiException(
                statutAbsence,
                "SESSION_INCONNUE",
                "La session demandée n'existe pas."
        ));
    }

    public SessionDetailResponse detail(Long id) {
        SessionCours session = require(id);
        return new SessionDetailResponse(
                session.getId(), session.getTitre(), session.getCode(), session.getPromotionId(),
                session.getOuvertureAt(), session.getExpirationAt(), session.getFinAt(), session.getClotureAt()
        );
    }

    public FinishSessionResponse finish(Long id) {
        SessionCours session = require(id);
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.GONE, "SESSION_CLOTUREE", "La session est clôturée.");
        }
        if (session.getFinAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_DEJA_TERMINEE", "La session est déjà terminée.");
        }
        Instant now = clock.instant();
        session.finish(now);
        return new FinishSessionResponse(session.getId(), session.getFinAt());
    }

    public CloseSessionResponse close(Long id) {
        SessionCours session = require(id);
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "SESSION_DEJA_CLOTUREE", "La session est déjà clôturée.");
        }
        Instant now = clock.instant();
        session.close(now, now);
        return new CloseSessionResponse(session.getId(), session.getFinAt(), session.getClotureAt());
    }

    public SessionCours requireByCode(String code) {
        return repository.findByCode(code).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "CODE_INCONNU",
                "Le code de présence est inconnu."
        ));
    }

    private String uniqueCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(8);
            for (int index = 0; index < 8; index++) {
                builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            code = builder.toString();
        } while (repository.findByCode(code).isPresent());
        return code;
    }
}
