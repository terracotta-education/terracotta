package edu.iu.terracotta.model;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "api_one_use_token")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ApiOneUseToken that = (ApiOneUseToken) o;

        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return 31 * (token != null ? token.hashCode() : 0);
    }

}
