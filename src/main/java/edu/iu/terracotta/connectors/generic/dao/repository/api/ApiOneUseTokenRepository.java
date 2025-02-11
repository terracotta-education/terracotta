package edu.iu.terracotta.connectors.generic.dao.repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOneUseToken;

import java.util.Date;

@Transactional
public interface ApiOneUseTokenRepository extends JpaRepository<ApiOneUseToken, Long> {

    ApiOneUseToken findByToken(String token);

    @Transactional
    void deleteByCreatedAtBefore(Date expiryDate);

}
