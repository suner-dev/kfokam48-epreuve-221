package com.kfokam48.presencerelecture.relecture.api;

import com.kfokam48.presencerelecture.relecture.application.ReviewTaskService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relectures")
public class ReviewTaskController {
    private final ReviewTaskService service;

    public ReviewTaskController(ReviewTaskService service) {
        this.service = service;
    }

    @GetMapping("/a-faire")
    public List<ReviewTaskResponse> findForReviewer(@RequestParam Long etudiantId) {
        return service.findForReviewer(etudiantId);
    }

    @PostMapping("/{id}/debut")
    public ReviewStartResponse start(@PathVariable Long id) {
        return service.start(id);
    }
}
