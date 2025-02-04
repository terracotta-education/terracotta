package edu.iu.terracotta.model.app.integrations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_integrations_token")
public class IntegrationToken extends BaseIntegrationEntity {

    @ManyToOne
    @JoinColumn(
        name = "lti_user_user_id",
        nullable = false
    )
    private LtiUserEntity user;

    @ManyToOne
    @JoinColumn(
        name = "integration_id",
        nullable = false
    )
    private Integration integration;

    @ManyToOne
    @JoinColumn(
        name = "submission_id",
        nullable = false
    )
    private Submission submission;

    @OneToMany(mappedBy = "integrationToken")
    private List<IntegrationTokenLog> logs;

    @Column private String token;
    @Column private String securedInfo;
    @Column private Timestamp redeemedAt;

    @Transient
    public boolean isExpired(int ttl) {
        return Timestamp.from(Instant.now()).after(new Timestamp(this.getCreatedAt().getTime() + (ttl * 1000L)));
    }

    @Transient
    public boolean isAlreadyRedeemed() {
        return redeemedAt != null;
    }

    @Transient
    public Optional<SecuredInfo> getSecuredInfo() {
        if (StringUtils.isBlank(securedInfo)) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ObjectMapper().readValue(securedInfo, SecuredInfo.class));
        } catch (JsonProcessingException e) {
            log.error("Error mapping json to SecuredInfo", e);
        }

        return Optional.empty();
    }

    public void setSecuredInfo(SecuredInfo securedInfo) {
        try {
            this.securedInfo = new ObjectMapper().writeValueAsString(securedInfo);
        } catch (JsonProcessingException e) {
            log.error("Error mapping SecuredInfo to json", e);
        }
    }

}
