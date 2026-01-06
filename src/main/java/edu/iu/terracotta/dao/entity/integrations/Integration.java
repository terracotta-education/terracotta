package edu.iu.terracotta.dao.entity.integrations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.dao.entity.Question;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_integrations_integration")
public class Integration extends BaseUuidEntity {

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
