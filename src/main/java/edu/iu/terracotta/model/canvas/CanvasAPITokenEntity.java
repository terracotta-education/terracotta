package edu.iu.terracotta.model.canvas;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.oauth2.APIToken;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "canvas_api_token")
public class CanvasAPITokenEntity implements APIToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "token_id",
        nullable = false
    )
    private long tokenId;

    @Column(nullable = false)
    private String accessToken;

    @Basic
    @Column(nullable = false)
    private String refreshToken;

    @Basic
    @Column(nullable = false)
    private Timestamp expiresAt;

    @Basic
    @Column(nullable = false)
    private Long canvasUserId;

    @Basic
    @Column(nullable = false)
    private String canvasUserName;

    @Lob
    @Column
    private String scopes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private LtiUserEntity user;

    public Set<String> getScopesAsSet() {
        if (this.scopes == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(this.scopes.split(" ")));
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
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        CanvasAPITokenEntity other = (CanvasAPITokenEntity) obj;

        return tokenId == other.tokenId;
    }

}
