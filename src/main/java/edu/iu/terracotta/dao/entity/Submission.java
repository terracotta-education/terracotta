package edu.iu.terracotta.dao.entity;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_submission")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Submission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "submission_id",
        nullable = false
    )
    private Long submissionId;

    @Column private Float calculatedGrade; //  grade calculated by points
    @Column private Float alteredCalculatedGrade; // calculated grade altered by instructor (i.e. for partial credit)
    @Column private Float totalAlteredGrade; // manual total altered grade (i.e. 0 for cheating)
    @Column private Timestamp dateSubmitted;
    @Column private boolean lateSubmission;
    @Column private boolean gradeOverridden;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "participant_id",
        nullable = false
    )
    private Participant participant;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "assessment_assessment_id",
        nullable = false
    )
    private Assessment assessment;

    @OneToMany(
        mappedBy = "submission",
        orphanRemoval = true
    )
    private List<QuestionSubmission> questionSubmissions;

    @OneToMany(
        mappedBy = "submission",
        orphanRemoval = true
    )
    private List<SubmissionComment> submissionComments;

    @OneToOne(mappedBy = "submission")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private IntegrationToken integrationToken;

    @Transient
    public Integration getIntegration() {
        return assessment.getIntegration();
    }

    @Transient
    public boolean isIntegration() {
        return assessment.isIntegration();
    }

    @Transient
    private String integrationLaunchUrl;

    @Transient
    public boolean isIntegrationFeedbackEnabled() {
        return assessment.canViewResponses();
    }

    @Transient
    public Timestamp getIntegrationTokenLaunchedAt() {
        if (integrationToken == null) {
            return null;
        }

        return integrationToken.getLastLaunchedAt();
    }

    @Transient
    public boolean isSubmitted() {
        return dateSubmitted != null;
    }

}
