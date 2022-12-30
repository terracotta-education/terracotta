package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "terr_question_mc")
public class QuestionMc extends Question {

    @Column(name = "randomize_answers", nullable = false)
    private boolean randomizeAnswers = false;

    public boolean isRandomizeAnswers() {
        return randomizeAnswers;
    }

    public void setRandomizeAnswers(boolean randomizeAnswers) {
        this.randomizeAnswers = randomizeAnswers;
    }
}
