package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;

@Table(name = "terr_question_submission")
@Entity
public class QuestionSubmission extends BaseEntity {
    @Column(name = "question_submission_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionSubmissionId;

    @JoinColumn(name = "submission_submission_id", nullable = false)
    @ManyToOne(optional = false)
    private Submission submission;

    @Column(name = "calculated_points")
    private Float calculatedPoints;

    @Column(name = "altered_grade")
    private Float alteredGrade;

    @JoinColumn(name = "question_question_id", nullable = false)
    @OneToOne(optional = false)
    private Question question;

    @OneToMany(mappedBy = "questionSubmission", orphanRemoval = true)
    private List<QuestionSubmissionComment> questionSubmissionComments;


    public Long getQuestionSubmissionId() { return questionSubmissionId; }

    public void setQuestionSubmissionId(Long questionSubmissionId) { this.questionSubmissionId = questionSubmissionId; }

    public Float getCalculatedPoints() { return calculatedPoints; }

    public void setCalculatedPoints(Float calculatedPoints) { this.calculatedPoints = calculatedPoints; }

    public Float getAlteredGrade() { return alteredGrade; }

    public void setAlteredGrade(Float alteredGrade) { this.alteredGrade = alteredGrade; }

    public Question getQuestion() { return question; }

    public void setQuestion(Question question) { this.question = question; }

    public Submission getSubmission() { return submission; }

    public void setSubmission(Submission submission) { this.submission = submission; }

    public List<QuestionSubmissionComment> getQuestionSubmissionComments() { return questionSubmissionComments; }

    public void setQuestionSubmissionComments(List<QuestionSubmissionComment> questionSubmissionComments) { this.questionSubmissionComments = questionSubmissionComments; }
}