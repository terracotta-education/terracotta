package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "terr_question")
@Inheritance(strategy = InheritanceType.JOINED)
public class Question extends BaseEntity {

    @Id
    @Column(name = "question_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Lob
    @Column
    private String html;

    @Column
    private Float points;

    @ManyToOne
    @JoinColumn(name = "assessment_assessment_id")
    private Assessment assessment;

    @Column
    private Integer questionOrder;

    @Enumerated(EnumType.STRING)
    @Column
    private QuestionTypes questionType;

}
