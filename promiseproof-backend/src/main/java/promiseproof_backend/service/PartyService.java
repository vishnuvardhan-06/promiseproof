package promiseproof_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.entity.Party;
import promiseproof_backend.repository.PartyRepository;
import promiseproof_backend.repository.PromiseRepository;

import java.util.List;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    private final PromiseRepository promiseRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public PartyService(
            PartyRepository partyRepository,
            PromiseRepository promiseRepository,
            CurrentUser currentUser,
            AuditService auditService) {

        this.partyRepository = partyRepository;
        this.promiseRepository = promiseRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    // Create party
    public Party createParty(Party party) {

        if (party.getName() == null ||
                party.getName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Party name is required"
            );
        }

        if (party.getAbbreviation() == null ||
                party.getAbbreviation().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Party abbreviation is required"
            );
        }

        party.setName(party.getName().trim());

        if (partyRepository.existsByName(party.getName())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Party already exists"
            );
        }

        Party saved = partyRepository.save(party);

        auditService.record(
                "PARTY_CREATED",
                "PARTY",
                saved.getId(),
                saved.getName(),
                currentUser.email(),
                currentUser.role(),
                null
        );

        return saved;
    }

    // Get all parties
    public List<Party> getAllParties() {

        return partyRepository.findAll();
    }

    // Get party by ID
    public Party getPartyById(Long id) {

        return partyRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Party not found"
                        )
                );
    }

    // Update party
    public Party updateParty(Long id, Party updatedParty) {

        Party existingParty = getPartyById(id);

        if (updatedParty.getName() == null ||
                updatedParty.getName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Party name is required"
            );
        }

        if (updatedParty.getAbbreviation() == null ||
                updatedParty.getAbbreviation().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Party abbreviation is required"
            );
        }

        String newName = updatedParty.getName().trim();

        if (partyRepository.existsByNameAndIdNot(newName, id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another party already uses this name"
            );
        }

        existingParty.setName(newName);

        existingParty.setAbbreviation(
                updatedParty.getAbbreviation().trim()
        );

        existingParty.setDescription(
                updatedParty.getDescription()
        );

        existingParty.setState(
                updatedParty.getState()
        );

        existingParty.setOfficialWebsite(
                updatedParty.getOfficialWebsite()
        );

        Party saved = partyRepository.save(existingParty);

        auditService.record(
                "PARTY_UPDATED",
                "PARTY",
                saved.getId(),
                saved.getName(),
                currentUser.email(),
                currentUser.role(),
                null
        );

        return saved;
    }

    // Delete party
    public void deleteParty(Long id) {

        Party party = getPartyById(id);

        long promiseCount = promiseRepository.countByParty_Id(id);

        if (promiseCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This party is linked to " + promiseCount +
                            " promise(s) and cannot be deleted. " +
                            "Reassign or remove those promises first."
            );
        }

        partyRepository.delete(party);

        auditService.record(
                "PARTY_DELETED",
                "PARTY",
                id,
                party.getName(),
                currentUser.email(),
                currentUser.role(),
                null
        );
    }
}