package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
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
import javax.persistence.Transient;

@Entity
@Getter
@Setter
@Table(name = "terr_consent_document")
public class ConsentDocument extends BaseEntity {

    public static final String COMPRESSED_FILE_EXTENSION = ".zip";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_document_id", nullable = false)
    private Long consentDocumentId;

    @Column
    private String title;

    @Column
    private String filePointer;

    @Lob
    @Column
    private String html;

    @OneToOne(mappedBy = "consentDocument")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Experiment experiment;

    @Column
    private String lmsAssignmentId;

    @Column
    private String resourceLinkId;

    @Column
    private String fileUri;

    @Column
    private String encryptionPhrase;

    @Column
    private String encryptionMethod;

    @Transient
    public boolean isCompressed() {
        return StringUtils.isNoneEmpty(encryptionMethod, encryptionPhrase, fileUri);
    }

    @Transient
    public String getEncodedFileName() {
        return StringUtils.substringAfterLast(fileUri, "/");
    }

    @Transient
    public String getEncryptedFileUri() {
        return String.format("%s%s", fileUri, COMPRESSED_FILE_EXTENSION);
    }

}
