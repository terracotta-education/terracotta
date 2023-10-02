package edu.iu.terracotta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.model.lti.NonceState;

import java.util.Date;

@Transactional
public interface NonceStateRepository extends JpaRepository<NonceState, String> {

    NonceState findByNonce(String nonce);
    NonceState findByStateHash(String stateHash);
    Boolean existsByNonce(String nonce);

    @Transactional
    void deleteByNonce(String nonce);

    @Transactional
    void deleteByCreatedAtBefore(Date expiryDate);

}
