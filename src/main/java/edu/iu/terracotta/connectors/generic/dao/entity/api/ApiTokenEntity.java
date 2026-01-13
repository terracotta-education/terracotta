package edu.iu.terracotta.connectors.generic.dao.entity.api;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ApiToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_token")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiTokenEntity implements ApiToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "token_id",
        nullable = false
    )
    private long tokenId;

    @Lob
    @Column
    private String scopes;

    @Column(nullable = false)
    private String accessToken;

    @Column(nullable = false)
    private String refreshToken;

    @Column(nullable = false)
    private Timestamp expiresAt;

    @Column(nullable = false)
    private Long lmsUserId;

    @Column(nullable = false)
    private String lmsUserName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private LtiUserEntity user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LmsConnector lmsConnector;

    public Set<String> getScopesAsSet() {
        if (this.scopes == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(this.scopes.split(" ")));
    }

}
