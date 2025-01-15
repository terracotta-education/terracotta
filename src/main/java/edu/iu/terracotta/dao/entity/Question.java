package edu.iu.terracotta.dao.entity;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.RegradeOption;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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

    @Column private Float points;
    @Column private Integer questionOrder;

    @Lob
    @Column
    private String html;

    @Column
    @Enumerated(EnumType.STRING)
    private QuestionTypes questionType;

    @Column
    @Enumerated(EnumType.STRING)
    private RegradeOption regradeOption;

    @OneToOne(mappedBy = "question")
    private Integration integration;

    @ManyToOne
    @JoinColumn(name = "assessment_assessment_id")
    private Assessment assessment;

    @Transient
    public boolean isIntegration() {
        return QuestionTypes.INTEGRATION == questionType;
    }

}
