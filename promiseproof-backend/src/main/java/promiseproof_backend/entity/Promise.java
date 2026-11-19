
package promiseproof_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promises")
public class Promise {

    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String REVIEWED = "REVIEWED";
    public static final String PUBLISHED = "PUBLISHED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Keep these columns temporarily for existing records.
    private String partyName;
    private String electionName;

    // New relationship with Party.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    // New relationship with Election.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "election_id")
    private Election election;

    private String state;

    // Public implementation status. Managed exclusively through the status
    // verification workflow (approved proposal or audited administrator
    // correction); promise create/update endpoints never set this directly.
    private String status;

    // Independent verification state, separate from the implementation
    // status. Tracks whether the recorded status is supported by reviewed
    // evidence.
    // Values: UNREVIEWED, UNDER_REVIEW, EVIDENCE_REVIEWED, STATUS_VERIFIED,
    // DISPUTED.
    @Column(length = 30)
    private String verificationState = "UNREVIEWED";

    // Publication workflow: DRAFT -> SUBMITTED -> REVIEWED -> PUBLISHED.
    // Legacy rows with a null value are treated as PUBLISHED so existing
    // public records do not disappear after the upgrade.
    @Column(length = 20)
    private String publicationStatus = DRAFT;

    @Column(columnDefinition = "TEXT")
    private String publicationComment;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime publishedAt;

    // Canonical promise metadata. The original wording and manifesto
    // references live in the separate PromiseSource records.
    @Column(length = 80)
    private String category;

    @Column(length = 200)
    private String target;

    private LocalDate targetDate;

    @Column(length = 120)
    private String geographicScope;

    // Who created the record, captured when a promise is first entered.
    @Column(length = 190)
    private String createdByEmail;

    @Column(length = 120)
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Promise() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = "NOT_ASSESSED";
        }

        if (verificationState == null) {
            verificationState = "UNREVIEWED";
        }

        if (publicationStatus == null) {
            publicationStatus = DRAFT;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public String getElectionName() {
        return electionName;
    }

    public void setElectionName(String electionName) {
        this.electionName = electionName;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationState() {
        return verificationState;
    }

    public void setVerificationState(String verificationState) {
        this.verificationState = verificationState;
    }

    public String getPublicationStatus() {
        return publicationStatus;
    }

    public void setPublicationStatus(String publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    /**
     * Legacy records without a publication status were public before the
     * workflow existed and remain public.
     */
    public boolean isPubliclyVisible() {
        return publicationStatus == null ||
                PUBLISHED.equals(publicationStatus);
    }

    public String getPublicationComment() {
        return publicationComment;
    }

    public void setPublicationComment(String publicationComment) {
        this.publicationComment = publicationComment;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getGeographicScope() {
        return geographicScope;
    }

    public void setGeographicScope(String geographicScope) {
        this.geographicScope = geographicScope;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
