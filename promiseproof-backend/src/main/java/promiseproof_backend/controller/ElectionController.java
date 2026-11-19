
package promiseproof_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import promiseproof_backend.entity.Election;
import promiseproof_backend.service.ElectionService;

import java.util.List;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {

    private final ElectionService electionService;

    public ElectionController(ElectionService electionService) {
        this.electionService = electionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Election createElection(@RequestBody Election election) {
        return electionService.createElection(election);
    }

    @GetMapping
    public List<Election> getAllElections() {
        return electionService.getAllElections();
    }

    @GetMapping("/{id}")
    public Election getElectionById(@PathVariable Long id) {
        return electionService.getElectionById(id);
    }

    @PutMapping("/{id}")
    public Election updateElection(
            @PathVariable Long id,
            @RequestBody Election election) {
        return electionService.updateElection(id, election);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteElection(@PathVariable Long id) {
        electionService.deleteElection(id);
    }
}