package promiseproof_backend.dto;

import java.time.LocalDateTime;

public class AuditRecordResponse {

    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String entityTitle;
    private String actorEmail;
    private String actorRole;
    private String comment;
    private LocalDateTime createdAt;

    public AuditRecordResponse() {
    }

    public AuditRecordResponse(
            String action,
            String entityType,
            Long entityId,
            String entityTitle,
            String actorEmail,
            String actorRole,
            String comment,
            LocalDateTime createdAt
    ) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityTitle = entityTitle;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEntityTitle() {
        return entityTitle;
    }

    public void setEntityTitle(String entityTitle) {
        this.entityTitle = entityTitle;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
