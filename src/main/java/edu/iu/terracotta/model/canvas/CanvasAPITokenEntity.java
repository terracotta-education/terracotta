package edu.iu.terracotta.model.canvas;

import java.sql.Timestamp;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.oauth2.APIToken;

@Entity
@Table(name = "canvas_api_token")
public class CanvasAPITokenEntity implements APIToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id", nullable = false)
    private long tokenId;

    @Basic
    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Basic
    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @Basic
    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;

    @Basic
    @Column(name = "canvas_user_id", nullable = false)
    private Long canvasUserId;

    @Basic
    @Column(name = "canvas_user_name", nullable = false)
    private String canvasUserName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private LtiUserEntity user;

    public long getTokenId() {
        return tokenId;
    }

    public void setTokenId(long tokenId) {
        this.tokenId = tokenId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getCanvasUserId() {
        return canvasUserId;
    }

    public void setCanvasUserId(Long canvasUserId) {
        this.canvasUserId = canvasUserId;
    }

    public String getCanvasUserName() {
        return canvasUserName;
    }

    public void setCanvasUserName(String canvasUserName) {
        this.canvasUserName = canvasUserName;
    }

    public LtiUserEntity getUser() {
        return user;
    }

    public void setUser(LtiUserEntity user) {
        this.user = user;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (tokenId ^ (tokenId >>> 32));
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
        CanvasAPITokenEntity other = (CanvasAPITokenEntity) obj;
        if (tokenId != other.tokenId)
            return false;
        return true;
    }

}
