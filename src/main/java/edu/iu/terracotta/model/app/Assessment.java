package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.List;

@Table(name = "terr_assessment")
@Entity
public class Assessment {
    @Column(name = "assessment_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assessmentId;

    @Column(name = "html")
    @Lob
    private String html;

    @OneToMany(mappedBy = "assessment", orphanRemoval = true)
    private List<Question> questions;

    @JoinColumn(name = "treatment_treatment_id", nullable = false)
    @OneToOne(optional = false)
    private Treatment treatment;

    @Column(name = "title")
    private String title;

    public Long getAssessmentId() { return assessmentId; }

    public void setAssessmentId(Long assessmentId) { this.assessmentId = assessmentId; }

    public String getHtml() { return html; }

    public void setHtml(String html) { this.html = html; }

    public List<Question> getQuestions() { return questions; }

    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public Treatment getTreatment() { return treatment; }

    public void setTreatment(Treatment treatment) { this.treatment = treatment; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }
}