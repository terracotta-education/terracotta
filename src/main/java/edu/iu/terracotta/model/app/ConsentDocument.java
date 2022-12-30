package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Table(name = "terr_consent_document")
@Entity
public class ConsentDocument extends BaseEntity {
    @Column(name = "consent_document_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long consentDocumentId;

    @Column(name = "title")
    private String title;

    @Column(name = "file_pointer")
    private String filePointer;

    @Column(name = "html")
    @Lob
    private String html;

    @OneToOne(mappedBy = "consentDocument")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Experiment experiment;

    @Column(name = "lms_assignment_id")
    private String lmsAssignmentId;

    @Column(name = "resource_link_id")
    private String resourceLinkId;

    public String getLmsAssignmentId() {
        return lmsAssignmentId;
    }

    public void setLmsAssignmentId(String lmsAssignmentId) {
        this.lmsAssignmentId = lmsAssignmentId;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getFilePointer() {
        return filePointer;
    }

    public void setFilePointer(String filePointer) {
        this.filePointer = filePointer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getConsentDocumentId() {
        return consentDocumentId;
    }

    public void setConsentDocumentId(Long consentDocumentId) {
        this.consentDocumentId = consentDocumentId;
    }

    public String getResourceLinkId() {
        return resourceLinkId;
    }

    public void setResourceLinkId(String resourceLinkId) {
        this.resourceLinkId = resourceLinkId;
    }
}