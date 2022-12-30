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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "iss_configuration")
public class PlatformDeployment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "key_id")
    private long keyId;
    @Basic
    @Column(name = "iss", nullable = false)
    private String iss;  //The value we receive in the issuer from the platform. We will use it to know where this come from.
    @Basic
    @Column(name = "client_id", nullable = false)
    private String clientId;  //A tool MUST thus allow multiple deployments on a given platform to share the same client_id
    @Basic
    @Column(name = "oidc_endpoint", nullable = false)
    private String oidcEndpoint;  // Where in the platform we need to ask for the oidc authentication.
    @Basic
    @Column(name = "jwks_endpoint")
    private String jwksEndpoint;  // Where in the platform we need to ask for the keys.
    @Basic
    @Column(name = "oAuth2_token_url")
    private String oAuth2TokenUrl;  // Where in the platform we need to ask for the oauth2 tokens
    @Basic
    @Column(name = "oAuth2_token_aud")
    private String oAuth2TokenAud;  // Sometimes, for example D2L, has a different aud for the tokens.
    @Basic
    @Column(name = "api_token")
    private String apiToken;
    @Basic
    @Column(name = "base_url")
    private String baseUrl;
    @Basic
    @Column(name = "caliper_configuration")
    private Boolean caliperConfiguration;

    @Basic
    @Column(name = "caliper_sensor_id")
    private String caliperSensorId;

    @Basic
    @Column(name = "caliper_client_id")
    private String caliperClientId;

    @Basic
    @Column(name = "caliper_api_key")
    private String caliperApiKey;

    @Basic
    @Column(name = "caliper_connection_timeout")
    private Integer caliperConnectionTimeout;

    @Basic
    @Column(name = "caliper_content_type")
    private String caliperContentType;

    @Basic
    @Column(name = "caliper_host")
    private String caliperHost;

    @Basic
    @Column(name = "caliper_socket_timeout")
    private Integer caliperSocketTimeOut;

    @Basic
    @Column(name = "enable_automatic_deployments", nullable = false)
    private Boolean enableAutomaticDeployments = false;

    @OneToMany(mappedBy = "platformDeployment", fetch = FetchType.LAZY)
    private Set<ToolDeployment> toolDeployments;


    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getOidcEndpoint() {
        return oidcEndpoint;
    }

    public void setOidcEndpoint(String oidcEndpoint) {
        this.oidcEndpoint = oidcEndpoint;
    }

    public String getJwksEndpoint() {
        return jwksEndpoint;
    }

    public void setJwksEndpoint(String jwksEndpoint) {
        this.jwksEndpoint = jwksEndpoint;
    }

    public String getoAuth2TokenUrl() {
        return oAuth2TokenUrl;
    }

    public void setoAuth2TokenUrl(String oAuth2TokenUrl) {
        this.oAuth2TokenUrl = oAuth2TokenUrl;
    }

    public String getoAuth2TokenAud() {
        return oAuth2TokenAud;
    }

    public void setoAuth2TokenAud(String oAuth2TokenAud) {
        this.oAuth2TokenAud = oAuth2TokenAud;
    }

    public Set<ToolDeployment> getToolDeployments() {
        return toolDeployments;
    }

    public void setToolDeployments(Set<ToolDeployment> toolDeployments) {
        this.toolDeployments = toolDeployments;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Boolean getCaliperConfiguration() {
        return caliperConfiguration;
    }

    public void setCaliperConfiguration(Boolean caliperConfiguration) {
        this.caliperConfiguration = caliperConfiguration;
    }

    public String getCaliperSensorId() {
        return caliperSensorId;
    }

    public void setCaliperSensorId(String caliperSensorId) {
        this.caliperSensorId = caliperSensorId;
    }

    public String getCaliperClientId() {
        return caliperClientId;
    }

    public void setCaliperClientId(String caliperClientId) {
        this.caliperClientId = caliperClientId;
    }

    public String getCaliperApiKey() {
        return caliperApiKey;
    }

    public void setCaliperApiKey(String caliperApiKey) {
        this.caliperApiKey = caliperApiKey;
    }

    public Integer getCaliperConnectionTimeout() {
        return caliperConnectionTimeout;
    }

    public void setCaliperConnectionTimeout(Integer caliperConnectionTimeout) {
        this.caliperConnectionTimeout = caliperConnectionTimeout;
    }

    public String getCaliperContentType() {
        return caliperContentType;
    }

    public void setCaliperContentType(String caliperContentType) {
        this.caliperContentType = caliperContentType;
    }

    public String getCaliperHost() {
        return caliperHost;
    }

    public void setCaliperHost(String caliperHost) {
        this.caliperHost = caliperHost;
    }

    public Integer getCaliperSocketTimeOut() {
        return caliperSocketTimeOut;
    }

    public void setCaliperSocketTimeOut(Integer caliperSocketTimeOut) {
        this.caliperSocketTimeOut = caliperSocketTimeOut;
    }

    public Boolean getEnableAutomaticDeployments() {
        return enableAutomaticDeployments;
    }

    public void setEnableAutomaticDeployments(Boolean enableAutomaticDeployments) {
        this.enableAutomaticDeployments = enableAutomaticDeployments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlatformDeployment that = (PlatformDeployment) o;

        if (keyId != that.keyId) return false;
        if (!Objects.equals(iss, that.iss)) return false;
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
