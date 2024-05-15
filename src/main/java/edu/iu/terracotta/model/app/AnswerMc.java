package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "terr_answer_mc")
public class AnswerMc extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "answer_id",
        nullable = false
    )
    private Long answerMcId;

    @Lob
    @Column
    private String html;

    @Column
    private Boolean correct;

    @ManyToOne
    @JoinColumn(name = "question_question_id")
    private Question question;

    @Column
    private Integer answerOrder;

}