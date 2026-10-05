package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface LtiContextRepository extends JpaRepository<LtiContextEntity, Long> {

    LtiContextEntity findByUuid(UUID uuid);

    LtiContextEntity findByContextKey(String key);
    LtiContextEntity findByContextKeyAndToolDeployment(String contextKey, ToolDeployment toolDeployment);

}
