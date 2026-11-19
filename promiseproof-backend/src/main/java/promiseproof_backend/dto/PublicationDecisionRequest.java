package promiseproof_backend.dto;

/**
 * Reviewer/administrator decision on the publication workflow.
 *
 * decision values:
 * - REVIEWED: source and wording checked; moves SUBMITTED -> REVIEWED.
 * - PUBLISHED: publishes a REVIEWED promise (visible on the public site).
 * - REJECTED: returns SUBMITTED/REVIEWED to DRAFT; a comment is required.
 */
public class PublicationDecisionRequest {

    private String decision;
    private String comment;

    public PublicationDecisionRequest() {
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
