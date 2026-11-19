package promiseproof_backend.dto;

public class PromiseStatusDecisionRequest {

    private String decision;
    private String comment;

    public PromiseStatusDecisionRequest() {
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
