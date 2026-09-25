package com.kfokam48.presencerelecture.tableau.application;

import com.kfokam48.presencerelecture.etudiant.domain.Etudiant;
import com.kfokam48.presencerelecture.etudiant.domain.EtudiantRepository;
import com.kfokam48.presencerelecture.exercice.domain.ExerciceRepository;
import com.kfokam48.presencerelecture.presence.domain.PresenceRepository;
import com.kfokam48.presencerelecture.promotion.application.PromotionService;
import com.kfokam48.presencerelecture.relecture.domain.RelectureRepository;
import com.kfokam48.presencerelecture.tableau.api.DashboardRow;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final PromotionService promotionService;
    private final EtudiantRepository etudiantRepository;
    private final PresenceRepository presenceRepository;
    private final ExerciceRepository exerciceRepository;
    private final RelectureRepository relectureRepository;

    public DashboardService(
            PromotionService promotionService,
            EtudiantRepository etudiantRepository,
            PresenceRepository presenceRepository,
            ExerciceRepository exerciceRepository,
            RelectureRepository relectureRepository
    ) {
        this.promotionService = promotionService;
        this.etudiantRepository = etudiantRepository;
        this.presenceRepository = presenceRepository;
        this.exerciceRepository = exerciceRepository;
        this.relectureRepository = relectureRepository;
    }

    public List<DashboardRow> findByPromotion(Long promotionId) {
        promotionService.require(promotionId);
        List<Etudiant> students = etudiantRepository.findByPromotionIdOrderByNomAscPrenomAsc(promotionId);
        Map<Long, Long> presenceCounts = countByStudent(presenceRepository.countByPromotion(promotionId));
        Map<Long, Long> exerciseCounts = countByStudent(exerciceRepository.countByPromotion(promotionId));
        Map<Long, Long> pendingCounts = countByStudent(relectureRepository.countPendingByPromotion(promotionId));
        Map<Long, Double> averages = averageByStudent(relectureRepository.averageByPromotion(promotionId));
        return students.stream()
                .map(student -> new DashboardRow(
                        student.getId(),
                        student.getNom(),
                        presenceCounts.getOrDefault(student.getId(), 0L),
                        exerciseCounts.getOrDefault(student.getId(), 0L),
                        averages.get(student.getId()),
                        pendingCounts.getOrDefault(student.getId(), 0L)
                ))
                .toList();
    }

    private Map<Long, Long> countByStudent(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    private Map<Long, Double> averageByStudent(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).doubleValue()
        ));
    }
}
