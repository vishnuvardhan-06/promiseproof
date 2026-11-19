package promiseproof_backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import promiseproof_backend.entity.Party;
import promiseproof_backend.service.PartyService;

import java.util.List;

@RestController
@RequestMapping("/api/parties")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    // Create party
    @PostMapping
    public ResponseEntity<Party> createParty(
            @RequestBody Party party) {

        Party savedParty = partyService.createParty(party);

        return new ResponseEntity<>(
                savedParty,
                HttpStatus.CREATED
        );
    }

    // Get all parties
    @GetMapping
    public ResponseEntity<List<Party>> getAllParties() {

        return ResponseEntity.ok(
                partyService.getAllParties()
        );
    }

    // Get party by ID
    @GetMapping("/{id}")
    public ResponseEntity<Party> getPartyById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                partyService.getPartyById(id)
        );
    }

    // Update party
    @PutMapping("/{id}")
    public ResponseEntity<Party> updateParty(
            @PathVariable Long id,
            @RequestBody Party party) {

        return ResponseEntity.ok(
                partyService.updateParty(id, party)
        );
    }

    // Delete party
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParty(
            @PathVariable Long id) {

        partyService.deleteParty(id);

        return ResponseEntity.noContent().build();
    }
}
