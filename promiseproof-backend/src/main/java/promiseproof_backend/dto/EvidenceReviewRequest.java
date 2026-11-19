package promiseproof_backend.dto;

public class EvidenceReviewRequest {

    // Legacy shortcut. When the three explicit review fields are absent this
    // maps APPROVED to all three positive decisions and REJECTED to a
    // document rejection.
    private String status;

    private String documentReview;
    private String sourceReview;
    private String claimReview;

    private String reviewComment;
    private String verificationNotes;

    public EvidenceReviewRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentReview() {
        return documentReview;
    }

    public void setDocumentReview(String documentReview) {
        this.documentReview = documentReview;
    }

    public String getSourceReview() {
        return sourceReview;
    }

    public void setSourceReview(String sourceReview) {
        this.sourceReview = sourceReview;
    }

    public String getClaimReview() {
        return claimReview;
    }

    public void setClaimReview(String claimReview) {
        this.claimReview = claimReview;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }
}
