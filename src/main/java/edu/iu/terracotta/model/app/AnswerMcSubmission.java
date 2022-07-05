package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Table(name = "terr_answer_mc_submission")
@Entity
public class AnswerMcSubmission extends BaseEntity {
    @Column(name = "answer_mc_sub_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerMcSubId;

    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    @ManyToOne(optional = false)
    private QuestionSubmission questionSubmission;

    @JoinColumn(name = "answer_mc_answer_id")
    @ManyToOne
    private AnswerMc answerMc;

    public Long getAnswerMcSubId() { return answerMcSubId; }

    public void setAnswerMcSubId(Long answerMcSubId) { this.answerMcSubId = answerMcSubId; }

    public QuestionSubmission getQuestionSubmission() { return questionSubmission; }

    public void setQuestionSubmission(QuestionSubmission questionSubmission) { this.questionSubmission = questionSubmission; }

    public AnswerMc getAnswerMc() { return answerMc; }

    public void setAnswerMc(AnswerMc answerMc) { this.answerMc = answerMc; }
}
