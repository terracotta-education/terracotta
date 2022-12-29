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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_answer_essay_submission")
public class AnswerEssaySubmission extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerEssaySubmissionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    private QuestionSubmission questionSubmission;

    @Lob
    @Column
    private String response;

}
