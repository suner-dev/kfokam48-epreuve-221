package com.kfokam48.presencerelecture.session.application;

import com.kfokam48.presencerelecture.common.exception.ApiException;
import com.kfokam48.presencerelecture.promotion.application.PromotionService;
import com.kfokam48.presencerelecture.session.api.CreateSessionRequest;
import com.kfokam48.presencerelecture.session.api.CreatedSessionResponse;
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
        promotionService.require(request.promotionId());
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
