package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.ToolDeployment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface LtiContextRepository extends JpaRepository<LtiContextEntity, Long> {

    LtiContextEntity findByContextKey(String key);
    LtiContextEntity findByContextKeyAndToolDeployment(String contextKey, ToolDeployment toolDeployment);

}
