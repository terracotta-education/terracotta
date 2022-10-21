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
@Table(name = "terr_answer_mc")
public class AnswerMc extends BaseEntity {

    @Id
    @Column(name = "answer_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
