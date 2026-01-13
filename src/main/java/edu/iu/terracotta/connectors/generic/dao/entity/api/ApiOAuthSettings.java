package edu.iu.terracotta.connectors.generic.dao.entity.api;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
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
@Table(name = "api_oauth_settings")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiOAuthSettings {

    @Id
    @Column(name = "settings_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long settingsId;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret;

    @Column(
        name = "oauth2_auth_url",
        nullable = false
    )
    private String oauth2AuthUrl;

    @Column(
        name = "oauth2_token_url",
        nullable = false
    )
    private String oauth2TokenUrl;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(
        name = "key_id",
        nullable = false
    )
    private PlatformDeployment platformDeployment;

}
