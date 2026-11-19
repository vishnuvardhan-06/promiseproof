
package promiseproof_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promise_id", nullable = false)
    private Promise promise;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String evidenceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 200)
    private String issuingDepartment;

    @Column(length = 120)
    private String documentReference;

    private LocalDate documentDate;

    @Column(length = 1000)
    private String claimScope;

    @Column(length = 64)
    private String sha256Hash;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, unique = true)
    private String storedFileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    // Three independent review decisions. The overall publication status is
    // derived from them; a genuine document is not automatically proof of the
    // specific claim.
    @Column(length = 20)
    private String documentReviewStatus = "PENDING";

    @Column(length = 20)
    private String sourceReviewStatus = "PENDING";

    @Column(length = 20)
    private String claimReviewStatus = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(length = 190)
    private String reviewedByEmail;

    @Column(length = 120)
    private String reviewedByName;

    // Who submitted the document, captured when it is uploaded.
    @Column(length = 190)
    private String uploadedByEmail;

    @Column(length = 120)
    private String uploadedByName;

    @Column(columnDefinition = "TEXT")
    private String reviewComment;

    private LocalDateTime uploadedAt;

    private LocalDateTime reviewedAt;

    public Evidence() {
    }

    @PrePersist
    public void onCreate() {
        uploadedAt = LocalDateTime.now();

        if (status == null) {
            status = "PENDING";
        }
    }

    public Long getId() {
        return id;
    }

    public Promise getPromise() {
        return promise;
    }

    public void setPromise(Promise promise) {
        this.promise = promise;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getIssuingDepartment() {
        return issuingDepartment;
    }

    public void setIssuingDepartment(String issuingDepartment) {
        this.issuingDepartment = issuingDepartment;
    }

    public String getDocumentReference() {
        return documentReference;
    }

    public void setDocumentReference(String documentReference) {
        this.documentReference = documentReference;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public String getClaimScope() {
        return claimScope;
    }

    public void setClaimScope(String claimScope) {
        this.claimScope = claimScope;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getDocumentReviewStatus() {
        return documentReviewStatus;
    }

    public void setDocumentReviewStatus(String documentReviewStatus) {
        this.documentReviewStatus = documentReviewStatus;
    }

    public String getSourceReviewStatus() {
        return sourceReviewStatus;
    }

    public void setSourceReviewStatus(String sourceReviewStatus) {
        this.sourceReviewStatus = sourceReviewStatus;
    }

    public String getClaimReviewStatus() {
        return claimReviewStatus;
    }

    public void setClaimReviewStatus(String claimReviewStatus) {
        this.claimReviewStatus = claimReviewStatus;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }

    public String getReviewedByEmail() {
        return reviewedByEmail;
    }

    public void setReviewedByEmail(String reviewedByEmail) {
        this.reviewedByEmail = reviewedByEmail;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUploadedByEmail() {
        return uploadedByEmail;
    }

    public void setUploadedByEmail(String uploadedByEmail) {
        this.uploadedByEmail = uploadedByEmail;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
