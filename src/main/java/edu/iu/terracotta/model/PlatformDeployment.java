/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "iss_configuration")
public class PlatformDeployment extends BaseEntity {

    public static final String LOCAL_URL = "https://test.terracotta.education";

    @Id
    @Column(name = "key_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long keyId;

    @Column(nullable = false)
    private String iss;  //The value we receive in the issuer from the platform. We will use it to know where this come from.

    @Column(nullable = false)
    private String clientId;  //A tool MUST thus allow multiple deployments on a given platform to share the same client_id

    @Column(nullable = false)
    private String oidcEndpoint;  // Where in the platform we need to ask for the oidc authentication.

    @Column
    private String jwksEndpoint;  // Where in the platform we need to ask for the keys.

    @Column(name = "oAuth2_token_url")
    private String oAuth2TokenUrl;  // Where in the platform we need to ask for the oauth2 tokens

    @Column(name = "oAuth2_token_aud")
    private String oAuth2TokenAud;  // Sometimes, for example D2L, has a different aud for the tokens.

    @Column
    private String apiToken;

    @Column
    private String baseUrl;

    @Column
    private Boolean caliperConfiguration;

    @Column
    private String caliperSensorId;

    @Column
    private String caliperClientId;

    @Column
    private String caliperApiKey;

    @Column
    private Integer caliperConnectionTimeout;

    @Column
    private String caliperContentType;

    @Column
    private String caliperHost;

    @Column
    private Integer caliperSocketTimeout;

    @Column(nullable = false)
    private Boolean enableAutomaticDeployments = false;

    @Column
    private String localUrl;

    @OneToMany(mappedBy = "platformDeployment", fetch = FetchType.LAZY)
    private Set<ToolDeployment> toolDeployments;

    public String getLocalUrl() {
        if (StringUtils.isBlank(localUrl)) {
            return LOCAL_URL;
        }

        return localUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlatformDeployment that = (PlatformDeployment) o;

        if (keyId != that.keyId) {
            return false;
        }

        if (!Objects.equals(iss, that.iss)) {
            return false;
        }

        return Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        int result = (int) keyId;
        result = 31 * result + (iss != null ? iss.hashCode() : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (oidcEndpoint != null ? oidcEndpoint.hashCode() : 0);
        return 31 * result + (oAuth2TokenUrl != null ? oAuth2TokenUrl.hashCode() : 0);
    }

}
