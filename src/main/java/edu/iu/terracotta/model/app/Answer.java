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

@Table(name = "terr_answer")
@Entity
public class Answer extends BaseEntity {
    @Column(name = "answer_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @Column(name = "html")
    @Lob
    private String html;

    @Column(name = "correct")
    private Boolean correct;

    @JoinColumn(name = "question_question_id")
    @ManyToOne
    private Question question;

    @Column(name = "answer_order")
    private Integer answerOrder;


    public Long getAnswerId() { return answerId; }

    public void setAnswerId(Long answerId) { this.answerId = answerId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public Boolean getCorrect() { return correct; }

    public void setCorrect(Boolean correct) { this.correct = correct; }

    public Question getQuestion() { return question; }

    public void setQuestion(Question question) { this.question = question; }

    public Integer getAnswerOrder() { return answerOrder; }

    public void setAnswerOrder(Integer answerOrder) { this.answerOrder = answerOrder; }
}