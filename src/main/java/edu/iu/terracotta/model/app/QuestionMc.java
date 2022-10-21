package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_question_mc")
public class QuestionMc extends Question {

    @Column(nullable = false)
    private boolean randomizeAnswers = false;

}
