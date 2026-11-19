package promiseproof_backend.dto;

public class PromiseStatusProposalRequest {

    private Long promiseId;
    private String proposedStatus;
    private String reason;
    private Long evidenceId;

    public PromiseStatusProposalRequest() {
    }

    public Long getPromiseId() {
        return promiseId;
    }

    public void setPromiseId(Long promiseId) {
        this.promiseId = promiseId;
    }

    public String getProposedStatus() {
        return proposedStatus;
    }

    public void setProposedStatus(String proposedStatus) {
        this.proposedStatus = proposedStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }
}
