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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "iss_configuration")
public class PlatformDeployment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "key_id")
    private long keyId;

    @Column(nullable = false)
    private String iss;  //The value we receive in the issuer from the platform. We will use it to know where this come from.

    @Column( nullable = false)
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
    private Integer caliperSocketTimeOut;

    @Column(nullable = false)
    private Boolean enableAutomaticDeployments = false;

    @OneToMany(mappedBy = "platformDeployment", fetch = FetchType.LAZY)
    private Set<ToolDeployment> toolDeployments;

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
        result = 31 * result + (oAuth2TokenUrl != null ? oAuth2TokenUrl.hashCode() : 0);

        return result;
    }

}
