package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.dao.entity.LtiNonce;

import java.util.Date;

@Transactional
public interface LtiNonceRepository extends JpaRepository<LtiNonce, Long> {

    /**
     * Deletes the nonce if present, consuming it so it can never be validated again.
     * @return the number of rows deleted: 1 if the nonce was found (and is therefore valid and unused), 0 otherwise
     */
    long deleteByNonce(String nonce);

    long deleteByCreatedAtBefore(Date expiryDate);

}
