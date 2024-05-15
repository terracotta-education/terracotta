package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.app.enumerator.RegradeOption;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "terr_question")
@Inheritance(strategy = InheritanceType.JOINED)
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "question_id",
        nullable = false
    )
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

    @Column
    @Enumerated(EnumType.STRING)
    private QuestionTypes questionType;

    @Column
    @Enumerated(EnumType.STRING)
    private RegradeOption regradeOption;

}
