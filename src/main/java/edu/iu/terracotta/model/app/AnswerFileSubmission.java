package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_answer_file_submission")
public class AnswerFileSubmission extends BaseEntity {

    public static final String COMPRESSED_FILE_EXTENSION = "zip";

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerFileSubmissionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    private QuestionSubmission questionSubmission;

    @Lob
    @Column
    private byte[] fileContent;

    @Column
    private String fileName;

    @Column
    private  String mimeType;

    @Column
    private String fileUri;

    @Column
    private String encryptionPhrase;

    @Column
    private String encryptionMethod;

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
        return String.format("%s.%s", fileUri, COMPRESSED_FILE_EXTENSION);
    }

}
