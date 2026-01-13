package edu.iu.terracotta.connectors.generic.dao.entity.api;

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
@Table(name = "api_one_use_token")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiOneUseToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "token_id",
        nullable = false
    )
    private long tokenId;

    @Column(
        nullable = false,
        length = 4096
    )
    private String token;

    /**
     * @param token the one use token
     */
    public ApiOneUseToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new AssertionError();
        }

        this.token = token;
    }

}
