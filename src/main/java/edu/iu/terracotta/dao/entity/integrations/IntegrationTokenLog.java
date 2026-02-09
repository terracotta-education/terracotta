package edu.iu.terracotta.dao.entity.integrations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.dao.model.enums.integrations.IntegrationTokenStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "terr_integrations_token_log")
public class IntegrationTokenLog extends BaseUuidEntity {

    public static final int ERROR_CODE_LENGTH = 6;

    private String score;
    private String error;
    private String token;
    private String code;

    @ManyToOne
    @JoinColumn(name = "token_id")
    private IntegrationToken integrationToken;

    @Enumerated(EnumType.STRING)
    private IntegrationTokenStatus status;

}
