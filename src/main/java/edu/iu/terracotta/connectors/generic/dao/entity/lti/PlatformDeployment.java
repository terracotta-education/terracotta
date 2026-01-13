package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.dao.entity.Feature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "iss_configuration")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDeployment extends BaseEntity {

    public static final String LOCAL_URL = "https://app.terracotta.education";

    @Id
    @Column(name = "key_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long keyId;

    @Column private String apiToken;
    @Column private String baseUrl;
    @Column private Boolean caliperConfiguration;
    @Column private String caliperSensorId;
    @Column private String caliperClientId;
    @Column private String caliperApiKey;
    @Column private Integer caliperConnectionTimeout;
    @Column private String caliperContentType;
    @Column private String caliperHost;
    @Column private Integer caliperSocketTimeout;
    @Column private String localUrl;
    @Column private String jwksEndpoint;  // Where in the platform we need to ask for the keys.

    @Column(nullable = false)
    private String iss;  //The value we receive in the issuer from the platform. We will use it to know where this come from.

    @Column(nullable = false)
    private String clientId;  //A tool MUST thus allow multiple deployments on a given platform to share the same client_id

    @Column(nullable = false)
    private String oidcEndpoint;  // Where in the platform we need to ask for the oidc authentication.

    @Column(name = "oAuth2_token_url")
    private String oAuth2TokenUrl;  // Where in the platform we need to ask for the oauth2 tokens

    @Column(name = "oAuth2_token_aud")
    private String oAuth2TokenAud;  // Sometimes, for example D2L, has a different aud for the tokens.

    @Builder.Default
    @Column(nullable = false)
    private Boolean enableAutomaticDeployments = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LmsConnector lmsConnector;

    @OneToMany(
        mappedBy = "platformDeployment",
        fetch = FetchType.LAZY
    )
    private Set<ToolDeployment> toolDeployments;

    @ManyToMany(mappedBy = "platformDeployments")
    private List<Feature> features;

    public String getLocalUrl() {
        if (StringUtils.isBlank(localUrl)) {
            return LOCAL_URL;
        }

        return localUrl;
    }

}
