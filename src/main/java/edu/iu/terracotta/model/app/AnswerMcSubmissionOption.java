package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import edu.iu.terracotta.model.BaseEntity;

@Table(name = "terr_answer_mc_submission_option")
@Entity
public class AnswerMcSubmissionOption extends BaseEntity {

    @Column(name = "answer_mc_sub_option_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long answerMcSubOptionId;

    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    @ManyToOne(optional = false)
    private QuestionSubmission questionSubmission;

    @JoinColumn(name = "answer_mc_answer_id", nullable = false)
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AnswerMc answerMc;

    @Column(name = "answer_order")
    private int answerOrder;

    public long getAnswerMcSubOptionId() {
        return answerMcSubOptionId;
    }

    public void setAnswerMcSubOptionId(long answerMcSubOptionId) {
        this.answerMcSubOptionId = answerMcSubOptionId;
    }

    public QuestionSubmission getQuestionSubmission() {
        return questionSubmission;
    }

    public void setQuestionSubmission(QuestionSubmission questionSubmission) {
        this.questionSubmission = questionSubmission;
    }

    public AnswerMc getAnswerMc() {
        return answerMc;
    }

    public void setAnswerMc(AnswerMc answerMc) {
        this.answerMc = answerMc;
    }

    public Integer getAnswerOrder() {
        return answerOrder;
    }

    public void setAnswerOrder(Integer answerOrder) {
        this.answerOrder = answerOrder;
    }

}
