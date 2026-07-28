package edu.iu.terracotta.dao.entity;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "lti_nonce")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LtiNonce extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(
        nullable = false,
        unique = true
    )
    private String nonce;

    /**
     * @param nonce the OIDC login-initiation nonce, persisted so it can be validated and consumed exactly
     *              once when the platform redirects back to the launch, without depending on a session.
     */
    public LtiNonce(String nonce) {
        if (StringUtils.isBlank(nonce)) {
            throw new AssertionError();
        }

        this.nonce = nonce;
    }

}
