package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Table(name = "terr_submission")
@Entity
public class Submission {
    @Column(name = "submission_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long submissionId;

    @JoinColumn(name = "participant_participant_id", nullable = false)
    @ManyToOne(optional = false)
    private Participant participant;

    @JoinColumn(name = "assessment_assessment_id", nullable = false)
    @ManyToOne(optional = false)
    private Assessment assessment;

    //grade calculated by points
    @Column(name = "calculated_grade")
    private Float calculatedGrade;

    //calculated grade altered by instructor (i.e. for partial credit)
    @Column(name = "altered_calculated_grade")
    private Float alteredCalculatedGrade;

    //manual total altered grade (i.e. 0 for cheating)
    @Column(name = "total_altered_grade")
    private Float totalAlteredGrade;

    @Column(name = "date_submitted")
    private Timestamp dateSubmitted;

    @Column(name = "late_submission")
    private boolean lateSubmission;

    @OneToMany(mappedBy = "submission", orphanRemoval = true)
    private List<QuestionSubmission> questionSubmissions;

    @OneToMany(mappedBy = "submission", orphanRemoval = true)
    private List<SubmissionComment> submissionComments;


    public Long getSubmissionId() { return submissionId; }

    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Participant getParticipant() { return participant; }

    public void setParticipant(Participant participant) { this.participant = participant; }

    public Assessment getAssessment() { return assessment; }

    public void setAssessment(Assessment assessment) { this.assessment = assessment; }

    public Float getCalculatedGrade() { return calculatedGrade; }

    public void setCalculatedGrade(Float calculatedGrade) { this.calculatedGrade = calculatedGrade; }

    public Float getAlteredCalculatedGrade() { return alteredCalculatedGrade; }

    public void setAlteredCalculatedGrade(Float alteredCalculatedGrade) { this.alteredCalculatedGrade = alteredCalculatedGrade; }

    public Float getTotalAlteredGrade() { return totalAlteredGrade; }

    public void setTotalAlteredGrade(Float totalAlteredGrade) { this.totalAlteredGrade = totalAlteredGrade; }

    public Timestamp getDateSubmitted() { return dateSubmitted; }

    public void setDateSubmitted(Timestamp dateSubmitted) { this.dateSubmitted = dateSubmitted; }

    public boolean getLateSubmission() { return lateSubmission; }

    public void setLateSubmission(boolean lateSubmission) { this.lateSubmission = lateSubmission; }

    public List<QuestionSubmission> getQuestionSubmissions() { return questionSubmissions; }

    public void setQuestionSubmissions(List<QuestionSubmission> questionSubmissions) { this.questionSubmissions = questionSubmissions; }

    public List<SubmissionComment> getSubmissionComments() { return submissionComments; }

    public void setSubmissionComments(List<SubmissionComment> submissionComments) { this.submissionComments = submissionComments; }
}