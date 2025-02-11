package edu.iu.terracotta.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_answer_file_submission")
public class AnswerFileSubmission extends BaseEntity {

    public static final String COMPRESSED_FILE_EXTENSION = ".zip";

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerFileSubmissionId;

    @Column private String fileName;
    @Column private  String mimeType;
    @Column private String fileUri;
    @Column private String encryptionPhrase;
    @Column private String encryptionMethod;

    @Lob
    @Column
    private byte[] fileContent;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "quest_sub_quest_sub_id",
        nullable = false
    )
    private QuestionSubmission questionSubmission;

    @Transient
    public boolean isCompressed() {
        return StringUtils.isNoneEmpty(encryptionMethod, encryptionPhrase);
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
