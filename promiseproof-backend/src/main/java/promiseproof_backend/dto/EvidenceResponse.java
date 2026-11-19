
package promiseproof_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EvidenceResponse {

    private Long id;
    private Long promiseId;
    private String promiseTitle;
    private String title;
    private String evidenceType;
    private String description;
    private String sourceUrl;
    private String issuingDepartment;
    private String documentReference;
    private LocalDate documentDate;
    private String claimScope;
    private String sha256Hash;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String status;
    private String uploadedByEmail;
    private String uploadedByName;
    private String reviewComment;
    private String documentReviewStatus;
    private String sourceReviewStatus;
    private String claimReviewStatus;
    private String verificationNotes;
    private String reviewedByEmail;
    private String reviewedByName;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;

    public EvidenceResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPromiseId() { return promiseId; }
    public void setPromiseId(Long promiseId) { this.promiseId = promiseId; }

    public String getPromiseTitle() { return promiseTitle; }
    public void setPromiseTitle(String promiseTitle) { this.promiseTitle = promiseTitle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getIssuingDepartment() { return issuingDepartment; }
    public void setIssuingDepartment(String issuingDepartment) { this.issuingDepartment = issuingDepartment; }

    public String getDocumentReference() { return documentReference; }
    public void setDocumentReference(String documentReference) { this.documentReference = documentReference; }

    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }

    public String getClaimScope() { return claimScope; }
    public void setClaimScope(String claimScope) { this.claimScope = claimScope; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUploadedByEmail() { return uploadedByEmail; }
    public void setUploadedByEmail(String uploadedByEmail) { this.uploadedByEmail = uploadedByEmail; }

    public String getUploadedByName() { return uploadedByName; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getDocumentReviewStatus() { return documentReviewStatus; }
    public void setDocumentReviewStatus(String documentReviewStatus) { this.documentReviewStatus = documentReviewStatus; }

    public String getSourceReviewStatus() { return sourceReviewStatus; }
    public void setSourceReviewStatus(String sourceReviewStatus) { this.sourceReviewStatus = sourceReviewStatus; }

    public String getClaimReviewStatus() { return claimReviewStatus; }
    public void setClaimReviewStatus(String claimReviewStatus) { this.claimReviewStatus = claimReviewStatus; }

    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }

    public String getReviewedByEmail() { return reviewedByEmail; }
    public void setReviewedByEmail(String reviewedByEmail) { this.reviewedByEmail = reviewedByEmail; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
