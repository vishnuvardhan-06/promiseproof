package promiseproof_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A traceable reference for a promise. A promise may have several sources
 * (election manifesto, official party document, government announcement,
 * implementation report). Every published promise must have at least one
 * source with the exact original quotation.
 */
@Entity
@Table(name = "promise_sources")
public class PromiseSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promise_id", nullable = false)
    private Promise promise;

    @Column(nullable = false, length = 200)
    private String manifestoName;

    @Column(length = 1000)
    private String sourceUrl;

    private Integer pageNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String exactQuotation;

    // MANIFESTO, PARTY_DOCUMENT, GOVERNMENT_ANNOUNCEMENT,
    // IMPLEMENTATION_REPORT, OTHER.
    @Column(length = 50)
    private String sourceType = "MANIFESTO";

    private LocalDate publishedDate;

    @Column(length = 190)
    private String createdByEmail;

    @Column(length = 120)
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PromiseSource() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (sourceType == null) {
            sourceType = "MANIFESTO";
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getManifestoName() {
        return manifestoName;
    }

    public void setManifestoName(String manifestoName) {
        this.manifestoName = manifestoName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getExactQuotation() {
        return exactQuotation;
    }

    public void setExactQuotation(String exactQuotation) {
        this.exactQuotation = exactQuotation;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDate publishedDate) {
        this.publishedDate = publishedDate;
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
