package edu.iu.terracotta.model.app.integrations;

import java.util.List;

import edu.iu.terracotta.model.app.Question;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_integrations_integration")
public class Integration extends BaseIntegrationEntity {

    public static final String UNLIMITED_ATTEMPTS_VALUE = "u";

    @JoinColumn(name = "question_id")
    @OneToOne(cascade = CascadeType.ALL)
    private Question question;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "configuration_id")
    private IntegrationConfiguration configuration;

    @OneToMany(mappedBy = "integration")
    private List<IntegrationToken> tokens;

    @Transient
    public boolean isFeedbackEnabled() {
        return configuration.isFeedbackEnabled();
    }

    @Transient
    public String getLaunchUrl() {
        return configuration.getLaunchUrl();
    }

    @Transient
    public String getLocalUrl() {
        return question
            .getAssessment()
            .getTreatment()
            .getCondition()
            .getExperiment()
            .getPlatformDeployment()
            .getLocalUrl();
    }

}
