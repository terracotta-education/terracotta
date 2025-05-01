package edu.iu.terracotta.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_question_mc")
public class QuestionMc extends Question {

    @Column(nullable = false)
    private boolean randomizeAnswers = false;

}
