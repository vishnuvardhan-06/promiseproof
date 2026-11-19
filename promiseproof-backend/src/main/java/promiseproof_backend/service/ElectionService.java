
package promiseproof_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.entity.Election;
import promiseproof_backend.repository.ElectionRepository;
import promiseproof_backend.repository.PromiseRepository;
import promiseproof_backend.service.AuditService;
import promiseproof_backend.service.CurrentUser;

import java.util.List;
import java.util.Set;

@Service
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final PromiseRepository promiseRepository;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    private static final Set<String> VALID_TYPES = Set.of(
            "LOK_SABHA",
            "STATE_ASSEMBLY",
            "LOCAL_BODY"
    );

    public ElectionService(
            ElectionRepository electionRepository,
            PromiseRepository promiseRepository,
            CurrentUser currentUser,
            AuditService auditService) {

        this.electionRepository = electionRepository;
        this.promiseRepository = promiseRepository;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    private void validateElection(Election election) {

        if (election.getName() == null ||
                election.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Election name is required"
            );
        }

        if (election.getState() == null ||
                election.getState().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "State is required"
            );
        }

        if (election.getElectionType() == null ||
                !VALID_TYPES.contains(
                        election.getElectionType().trim().toUpperCase()
                )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid election type"
            );
        }

        if (election.getYear() == null ||
                election.getYear() < 1900 ||
                election.getYear() > 2100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid election year"
            );
        }

        election.setName(election.getName().trim());
        election.setState(election.getState().trim());
        election.setElectionType(
                election.getElectionType().trim().toUpperCase()
        );
    }

    public Election createElection(Election election) {

        validateElection(election);

        boolean exists =
                electionRepository
                        .existsByNameIgnoreCaseAndStateIgnoreCaseAndYear(
                                election.getName(),
                                election.getState(),
                                election.getYear()
                        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This election already exists"
            );
        }

        Election saved = electionRepository.save(election);

        auditService.record(
                "ELECTION_CREATED",
                "ELECTION",
                saved.getId(),
                saved.getName(),
                currentUser.email(),
                currentUser.role(),
                null
        );

        return saved;
    }

    public List<Election> getAllElections() {
        return electionRepository.findAll();
    }

    public Election getElectionById(Long id) {
        return electionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Election not found"
                ));
    }

    public Election updateElection(Long id, Election updatedElection) {

        Election existing = getElectionById(id);

        validateElection(updatedElection);

        boolean duplicate =
                electionRepository
                        .existsByNameIgnoreCaseAndStateIgnoreCaseAndYearAndIdNot(
                                updatedElection.getName(),
                                updatedElection.getState(),
                                updatedElection.getYear(),
                                id
                        );

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another election with these details already exists"
            );
        }

        existing.setName(updatedElection.getName());
        existing.setElectionType(updatedElection.getElectionType());
        existing.setState(updatedElection.getState());
        existing.setYear(updatedElection.getYear());
        existing.setElectionDate(updatedElection.getElectionDate());
        existing.setDescription(updatedElection.getDescription());

        Election saved = electionRepository.save(existing);

        auditService.record(
                "ELECTION_UPDATED",
                "ELECTION",
                saved.getId(),
                saved.getName(),
                currentUser.email(),
                currentUser.role(),
                null
        );

        return saved;
    }

    public void deleteElection(Long id) {

        Election election = getElectionById(id);

        long promiseCount = promiseRepository.countByElection_Id(id);

        if (promiseCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This election is linked to " + promiseCount +
                            " promise(s) and cannot be deleted. " +
                            "Reassign or remove those promises first."
            );
        }

        electionRepository.delete(election);

        auditService.record(
                "ELECTION_DELETED",
                "ELECTION",
                id,
                election.getName(),
                currentUser.email(),
                currentUser.role(),
                null
        );
    }
}