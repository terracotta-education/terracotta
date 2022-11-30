package edu.iu.terracotta.model.canvas;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.iu.terracotta.model.PlatformDeployment;

@Entity
@Table(name = "canvas_api_oauth_settings")
public class CanvasAPIOAuthSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settings_id")
    private long settingsId;

    @Basic
    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Basic
    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Basic
    @Column(name = "oauth2_auth_url", nullable = false)
    private String oauth2AuthUrl;

    @Basic
    @Column(name = "oauth2_token_url", nullable = false)
    private String oauth2TokenUrl;

    @JsonIgnore
    @JoinColumn(name = "key_id", nullable = false)
    @OneToOne(optional = false)
    private PlatformDeployment platformDeployment;

    public long getSettingsId() {
        return settingsId;
    }

    public void setSettingsId(long settingsId) {
        this.settingsId = settingsId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOauth2AuthUrl() {
        return oauth2AuthUrl;
    }

    public void setOauth2AuthUrl(String oauth2AuthUrl) {
        this.oauth2AuthUrl = oauth2AuthUrl;
    }

    public String getOauth2TokenUrl() {
        return oauth2TokenUrl;
    }

    public void setOauth2TokenUrl(String oauth2TokenUrl) {
        this.oauth2TokenUrl = oauth2TokenUrl;
    }

    public PlatformDeployment getPlatformDeployment() {
        return platformDeployment;
    }

    public void setPlatformDeployment(PlatformDeployment platformDeployment) {
        this.platformDeployment = platformDeployment;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (settingsId ^ (settingsId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CanvasAPIOAuthSettings other = (CanvasAPIOAuthSettings) obj;
        if (settingsId != other.settingsId)
            return false;
        return true;
    }
}
