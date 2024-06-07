package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.LtiResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface LtiResultRepository extends JpaRepository<LtiResultEntity, Long> {

    /**
     * @param resultId the unique resultId key
     * @return the LtiResultEntity OR null if there is no entity matching this key
     */
    LtiResultEntity findByResultId(Long resultId);

}
