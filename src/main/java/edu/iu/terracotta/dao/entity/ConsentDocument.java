package edu.iu.terracotta.dao.entity;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Getter
@Setter
@Table(name = "terr_consent_document")
public class ConsentDocument extends BaseEntity {

    public static final String COMPRESSED_FILE_EXTENSION = ".zip";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "consent_document_id",
        nullable = false
    )
    private Long consentDocumentId;

    @Column private String title;
    @Column private String filePointer;
    @Column private String lmsAssignmentId;
    @Column private String resourceLinkId;
    @Column private String fileUri;
    @Column private String encryptionPhrase;
    @Column private String encryptionMethod;

    @Lob
    @Column
    private String html;

    @OneToOne(mappedBy = "consentDocument")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Experiment experiment;

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
