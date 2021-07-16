package edu.iu.terracotta.model.app.dto;

public class ConsentDto {

    private Long consentDocumentId;
    private String title;
    private String filePointer;
    private String html;

    public Long getConsentDocumentId() {
        return consentDocumentId;
    }

    public void setConsentDocumentId(Long consentDocumentId) {
        this.consentDocumentId = consentDocumentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePointer() {
        return filePointer;
    }

    public void setFilePointer(String filePointer) {
        this.filePointer = filePointer;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
