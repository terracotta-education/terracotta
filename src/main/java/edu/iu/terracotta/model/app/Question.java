package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Table(name = "terr_question")
@Entity
public class Question {
    @Column(name = "question_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(name = "html")
    @Lob
    private String html;

    @OneToMany(mappedBy = "question", orphanRemoval = true)
    private List<Answer> answers;

    @Column(name = "points")
    private Float points;

    @JoinColumn(name = "assessment_assessment_id")
    @ManyToOne
    private Assessment assessment;


    public Long getQuestionId() { return questionId; }

    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public List<Answer> getAnswers() { return answers; }

    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    public Float getPoints() { return points; }

    public void setPoints(Float points) { this.points = points; }

    public Assessment getAssessment() { return assessment; }

    public void setAssessment(Assessment assessment) { this.assessment = assessment; }
}