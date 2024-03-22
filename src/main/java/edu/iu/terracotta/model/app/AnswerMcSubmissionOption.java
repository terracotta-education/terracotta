package edu.iu.terracotta.model.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_answer_mc_submission_option")
public class AnswerMcSubmissionOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_mc_sub_option_id", nullable = false)
    private long answerMcSubOptionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    private QuestionSubmission questionSubmission;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "answer_mc_answer_id", nullable = false)
    private AnswerMc answerMc;

    @Column
    private int answerOrder;

}
