package edu.iu.terracotta.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_consent_document")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentDocument extends BaseEntity {

    public static final String COMPRESSED_FILE_EXTENSION = ".zip";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long consentDocumentId;

    private String title;
    private String filePointer;
    private String lmsAssignmentId;
    private String resourceLinkId;
    private String fileUri;
    private String encryptionPhrase;
    private String encryptionMethod;
    private String metadata; // JSON metadata from the LMS

    @Lob
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
