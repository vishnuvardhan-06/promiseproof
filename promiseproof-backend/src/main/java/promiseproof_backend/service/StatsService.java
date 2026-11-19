package promiseproof_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import promiseproof_backend.dto.ProgressGroupResponse;
import promiseproof_backend.dto.StatsOverviewResponse;
import promiseproof_backend.entity.Election;
import promiseproof_backend.entity.Evidence;
import promiseproof_backend.entity.Party;
import promiseproof_backend.entity.Promise;
import promiseproof_backend.repository.ElectionRepository;
import promiseproof_backend.repository.EvidenceRepository;
import promiseproof_backend.repository.PartyRepository;
import promiseproof_backend.repository.PromiseRepository;
import promiseproof_backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StatsService {

    private static final List<String> STATUSES = List.of(
            "NOT_ASSESSED",
            "NOT_STARTED",
            "IN_PROGRESS",
            "COMPLETED",
            "PARTIALLY_COMPLETED",
            "DELAYED",
            "CANCELLED"
    );

    private static final List<String> VERIFICATION_STATES = List.of(
            "UNREVIEWED",
            "UNDER_REVIEW",
            "EVIDENCE_REVIEWED",
            "STATUS_VERIFIED",
            "DISPUTED"
    );

    private final PromiseRepository promiseRepository;
    private final EvidenceRepository evidenceRepository;
    private final PartyRepository partyRepository;
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;

    public StatsService(
            PromiseRepository promiseRepository,
            EvidenceRepository evidenceRepository,
            PartyRepository partyRepository,
            ElectionRepository electionRepository,
            UserRepository userRepository) {

        this.promiseRepository = promiseRepository;
        this.evidenceRepository = evidenceRepository;
        this.partyRepository = partyRepository;
        this.electionRepository = electionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public StatsOverviewResponse getOverview() {

        List<Promise> promises = promiseRepository.findAll()
                .stream()
                .filter(Promise::isPubliclyVisible)
                .toList();

        Set<Long> publicPromiseIds = new HashSet<>();

        for (Promise promise : promises) {
            if (promise.getId() != null) {
                publicPromiseIds.add(promise.getId());
            }
        }

        List<Evidence> evidence = evidenceRepository.findAll()
                .stream()
                .filter(item -> item.getPromise() != null &&
                        publicPromiseIds.contains(item.getPromise().getId()))
                .toList();

        Map<String, Long> statusCounts = emptyStatusCounts();
        Map<String, Long> verificationStateCounts =
                emptyVerificationStateCounts();

        Map<String, Group> partyGroups = new LinkedHashMap<>();
        Map<String, Group> electionGroups = new LinkedHashMap<>();
        Map<String, Group> stateGroups = new LinkedHashMap<>();
        Set<String> states = new HashSet<>();

        for (Promise promise : promises) {

            String status = promise.getStatus() == null
                    ? "NOT_ASSESSED"
                    : promise.getStatus();

            statusCounts.merge(status, 1L, Long::sum);

            String verificationState =
                    promise.getVerificationState() == null
                            ? "UNREVIEWED"
                            : promise.getVerificationState();

            verificationStateCounts.merge(
                    verificationState,
                    1L,
                    Long::sum
            );

            // Party grouping
            if (promise.getParty() != null) {
                Party party = promise.getParty();
                accum(
                        partyGroups,
                        "party-" + party.getId(),
                        party.getId(),
                        party.getName(),
                        status
                );
            } else {
                String name = isBlank(promise.getPartyName())
                        ? "Unattributed"
                        : promise.getPartyName();

                accum(partyGroups, "party-name-" + name, null, name, status);
            }

            // Election grouping
            if (promise.getElection() != null) {
                Election election = promise.getElection();
                accum(
                        electionGroups,
                        "election-" + election.getId(),
                        election.getId(),
                        election.getName(),
                        status
                );
            } else {
                String name = isBlank(promise.getElectionName())
                        ? "Unattributed"
                        : promise.getElectionName();

                accum(
                        electionGroups,
                        "election-name-" + name,
                        null,
                        name,
                        status
                );
            }

            // State grouping
            if (isBlank(promise.getState())) {
                accum(stateGroups, "state-none", null, "Unspecified", status);
            } else {
                String stateName = promise.getState().trim();
                states.add(stateName.toLowerCase());
                accum(stateGroups, "state-" + stateName, null, stateName, status);
            }
        }

        long approvedEvidence = 0;
        long pendingEvidence = 0;
        long rejectedEvidence = 0;
        Set<Long> approvedPromiseIds = new HashSet<>();

        for (Evidence item : evidence) {

            String status = item.getStatus();

            if ("APPROVED".equals(status)) {
                approvedEvidence++;
                approvedPromiseIds.add(item.getPromise().getId());
            } else if ("PENDING".equals(status)) {
                pendingEvidence++;
            } else if ("REJECTED".equals(status)) {
                rejectedEvidence++;
            }
        }

        long totalPromises = promises.size();

        return new StatsOverviewResponse(
                totalPromises,
                partyRepository.count(),
                electionRepository.count(),
                userRepository.count(),
                evidence.size(),
                approvedEvidence,
                pendingEvidence,
                rejectedEvidence,
                approvedPromiseIds.size(),
                totalPromises - approvedPromiseIds.size(),
                states.size(),
                statusCounts,
                verificationStateCounts,
                toGroups(partyGroups),
                toGroups(electionGroups),
                toGroups(stateGroups)
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Map<String, Long> emptyStatusCounts() {

        Map<String, Long> counts = new LinkedHashMap<>();

        for (String status : STATUSES) {
            counts.put(status, 0L);
        }

        return counts;
    }

    private static Map<String, Long> emptyVerificationStateCounts() {

        Map<String, Long> counts = new LinkedHashMap<>();

        for (String state : VERIFICATION_STATES) {
            counts.put(state, 0L);
        }

        return counts;
    }

    private static void accum(
            Map<String, Group> groups,
            String key,
            Long id,
            String name,
            String status) {

        Group group = groups.get(key);

        if (group == null) {
            group = new Group(id, name);
            groups.put(key, group);
        }

        group.total++;
        group.statusCounts.merge(status, 1L, Long::sum);
    }

    private static List<ProgressGroupResponse> toGroups(
            Map<String, Group> groups) {

        List<ProgressGroupResponse> responses = new ArrayList<>();

        for (Group group : groups.values()) {
            responses.add(new ProgressGroupResponse(
                    group.id,
                    group.name,
                    group.total,
                    group.statusCounts
            ));
        }

        responses.sort(
                Comparator.comparingLong(ProgressGroupResponse::total)
                        .reversed()
                        .thenComparing(
                                ProgressGroupResponse::name,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        return responses;
    }

    private static final class Group {

        private final Long id;
        private final String name;
        private long total;
        private final Map<String, Long> statusCounts = emptyStatusCounts();

        private Group(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
