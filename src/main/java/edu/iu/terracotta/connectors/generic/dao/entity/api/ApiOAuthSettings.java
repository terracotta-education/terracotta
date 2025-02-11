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

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "api_oauth_settings")
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
