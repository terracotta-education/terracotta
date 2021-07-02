package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Table(name = "terr_question")
@Entity
public class Question extends BaseEntity {
    @Column(name = "question_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(name = "html")
    @Lob
    private String html;

    @Column(name = "points")
    private Float points;

    @JoinColumn(name = "assessment_assessment_id")
    @ManyToOne
    private Assessment assessment;

    @Column(name = "question_order")
    private Integer questionOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionTypes questionType;


    public Long getQuestionId() { return questionId; }

    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public Float getPoints() { return points; }

    public void setPoints(Float points) { this.points = points; }

    public Assessment getAssessment() { return assessment; }

    public void setAssessment(Assessment assessment) { this.assessment = assessment; }

    public Integer getQuestionOrder() { return questionOrder; }

    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }

    public QuestionTypes getQuestionType() { return questionType; }

    public void setQuestionType(QuestionTypes questionType) { this.questionType = questionType; }
}