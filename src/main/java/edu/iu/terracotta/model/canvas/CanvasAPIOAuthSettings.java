package edu.iu.terracotta.model.canvas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import edu.iu.terracotta.model.PlatformDeployment;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "canvas_api_oauth_settings")
public class CanvasAPIOAuthSettings {

    @Id
    @Column(name = "settings_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long settingsId;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String clientSecret;

    @Column(name = "oauth2_auth_url", nullable = false)
    private String oauth2AuthUrl;

    @Column(name = "oauth2_token_url", nullable = false)
    private String oauth2TokenUrl;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(name = "key_id", nullable = false)
    private PlatformDeployment platformDeployment;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (settingsId ^ (settingsId >>> 32));

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        CanvasAPIOAuthSettings other = (CanvasAPIOAuthSettings) obj;

        return settingsId == other.settingsId;
    }

}
