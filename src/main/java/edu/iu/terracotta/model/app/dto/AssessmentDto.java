package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.iu.terracotta.model.app.Question;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentDto {

    private Long assessmentId;
    private String html;
    private List<QuestionDto> questions;
    private Long treatmentId;
    private String title;

    public Long getAssessmentId() { return assessmentId; }

    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }

    public Long getTreatmentId() { return treatmentId; }

    public void setTreatmentId(Long treatmentId) { this.treatmentId = treatmentId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }
}
