package edu.iu.terracotta.model.lti;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import edu.iu.terracotta.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "nonce_state")
public class NonceState extends BaseEntity {

    @Id
    @Column(nullable = false)
    private String nonce;

    @Column(nullable = false)
    private String stateHash;


    @Column(nullable = false)
    private String state;

    @Column
    private String ltiStorageTarget;

    /**
     * @param nonce the nonce
     * @param stateHash the state_hash
     * @param state the state
     */
    public NonceState(String nonce, String stateHash, String state, String ltiStorageTarget) {
        if (StringUtils.isAnyBlank(nonce, stateHash, state)) {
            throw new AssertionError();
        }

        this.nonce = nonce;
        this.stateHash = stateHash;
        this.state = state;
        this.ltiStorageTarget = ltiStorageTarget;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NonceState that = (NonceState) o;

        return Objects.equals(nonce, that.nonce) &&
                Objects.equals(stateHash, that.stateHash) &&
                Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return 31 * (nonce != null ? nonce.hashCode() : 0);
    }

}
