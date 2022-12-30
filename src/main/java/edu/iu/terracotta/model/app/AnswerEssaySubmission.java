package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Table(name = "terr_answer_essay_submission")
@Entity
public class AnswerEssaySubmission extends BaseEntity {
    @Column(name = "answer_essay_submission_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerEssaySubmissionId;

    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    @ManyToOne(optional = false)
    private QuestionSubmission questionSubmission;

    @Column(name = "response")
    @Lob
    private String response;


    public QuestionSubmission getQuestionSubmission() { return questionSubmission; }

    public void setQuestionSubmission(QuestionSubmission questionSubmission) { this.questionSubmission = questionSubmission; }

    public String getResponse() { return response; }

    public void setResponse(String response) { this.response = response; }

    public Long getAnswerEssaySubmissionId() { return answerEssaySubmissionId; }

    public void setAnswerEssaySubmissionId(Long answerEssaySubmissionId) { this.answerEssaySubmissionId = answerEssaySubmissionId; }
}