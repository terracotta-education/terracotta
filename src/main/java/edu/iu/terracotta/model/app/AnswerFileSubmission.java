package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import javax.persistence.*;

@Table(name = "terr_answer_file_submission")
@Entity
public class AnswerFileSubmission extends BaseEntity {

    @Column(name = "answer_file_submission_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerFileSubmissionId;

    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    @ManyToOne(optional = false)
    private QuestionSubmission questionSubmission;


    @Column(name = "file_content",  columnDefinition="CLOB")
    @Lob
    private String file;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "mime_type")
    private  String mimeType;

    @Column(name= "file_uri")
    private String fileURI;

    public QuestionSubmission getQuestionSubmission() { return questionSubmission; }

    public void setQuestionSubmission(QuestionSubmission questionSubmission) { this.questionSubmission = questionSubmission; }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Long getAnswerFileSubmissionId() { return answerFileSubmissionId; }

    public void setAnswerFileSubmissionId(Long answerFileSubmissionId) { this.answerFileSubmissionId = answerFileSubmissionId; }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }


    public String getFileURI() {
        return fileURI;
    }

    public void setFileURI(String fileURI) {
        this.fileURI = fileURI;
    }
}
