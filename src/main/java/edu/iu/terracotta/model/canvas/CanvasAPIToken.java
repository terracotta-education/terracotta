package edu.iu.terracotta.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.iu.terracotta.model.oauth2.APIToken;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CanvasAPIToken implements APIToken {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("expires_in")
    private int expiresIn;
    private CanvasAPIUser user;

    public CanvasAPIToken() {
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public CanvasAPIUser getUser() {
        return user;
    }

    public void setUser(CanvasAPIUser user) {
        this.user = user;
    }

}
