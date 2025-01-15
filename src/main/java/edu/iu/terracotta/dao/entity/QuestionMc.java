package edu.iu.terracotta.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_question_mc")
public class QuestionMc extends Question {

    @Column(nullable = false)
    private boolean randomizeAnswers = false;

}
