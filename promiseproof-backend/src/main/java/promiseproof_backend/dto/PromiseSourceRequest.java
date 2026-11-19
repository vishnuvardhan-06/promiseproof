package promiseproof_backend.dto;

import java.time.LocalDate;

public class PromiseSourceRequest {

    private String manifestoName;
    private String sourceUrl;
    private Integer pageNumber;
    private String exactQuotation;
    private String sourceType;
    private LocalDate publishedDate;

    public PromiseSourceRequest() {
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
}
