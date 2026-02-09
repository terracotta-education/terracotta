package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import jakarta.persistence.Entity;
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
@Table(name = "lti_deep_link")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LtiDeepLink extends BaseUuidEntity {

    private String token;
    private String nonce;
    private String state;
    private String idToken;
    private String returnUrl;

}
