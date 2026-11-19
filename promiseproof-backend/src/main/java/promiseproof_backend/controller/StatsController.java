package promiseproof_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import promiseproof_backend.dto.StatsOverviewResponse;
import promiseproof_backend.service.StatsService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    // Public dashboard statistics. The service filters to published promises
    // and never returns reviewer identities or moderation details.
    @GetMapping("/overview")
    public ResponseEntity<StatsOverviewResponse> getOverview() {

        return ResponseEntity.ok(statsService.getOverview());
    }

    // Explicit public alias used by the citizen dashboard.
    @GetMapping("/public")
    public ResponseEntity<StatsOverviewResponse> getPublicOverview() {

        return ResponseEntity.ok(statsService.getOverview());
    }
}
