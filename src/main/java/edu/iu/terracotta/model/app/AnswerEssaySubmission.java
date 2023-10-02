package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_answer_essay_submission")
public class AnswerEssaySubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_essay_submission_id", nullable = false)
    private Long answerEssaySubmissionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    private QuestionSubmission questionSubmission;

    @Lob
    @Column
    private String response;

}