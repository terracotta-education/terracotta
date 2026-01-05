package edu.iu.terracotta.dao.entity.integrations;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Submission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_integrations_token")
public class IntegrationToken extends BaseUuidEntity {

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

    @OneToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @OneToMany(mappedBy = "integrationToken")
    private List<IntegrationTokenLog> logs;

    @Column private String token;
    @Column private String securedInfo;
    @Column private Timestamp lastLaunchedAt;
    @Column private Timestamp redeemedAt;

    @Transient
    public boolean isExpired(int ttl) {
        return ttl >= 1 && Timestamp.from(Instant.now()).after(new Timestamp(this.getLastLaunchedAt().getTime() + (ttl * 1000L)));
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
            return Optional.of(
                JsonMapper.builder()
                    .build()
                    .readValue(
                        securedInfo,
                        SecuredInfo.class
                    )
            );
        } catch (JacksonException e) {
            log.error("Error mapping json to SecuredInfo", e);
        }

        return Optional.empty();
    }

    public void setSecuredInfo(SecuredInfo securedInfo) {
        try {
            this.securedInfo = JsonMapper.builder()
                .build()
                .writeValueAsString(securedInfo);
        } catch (JacksonException e) {
            log.error("Error mapping SecuredInfo to json", e);
        }
    }

}
