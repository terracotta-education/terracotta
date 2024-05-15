package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.ApiOneUseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Transactional
public interface ApiOneUseTokenRepository extends JpaRepository<ApiOneUseToken, Long> {

    ApiOneUseToken findByToken(String token);

    @Transactional
    void deleteByCreatedAtBefore(Date expiryDate);

}
