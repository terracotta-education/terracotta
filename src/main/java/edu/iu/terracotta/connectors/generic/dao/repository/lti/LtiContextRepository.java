package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import jakarta.persistence.LockModeType;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface LtiContextRepository extends JpaRepository<LtiContextEntity, Long> {

    LtiContextEntity findByContextKey(String key);
    LtiContextEntity findByContextKeyAndToolDeployment(String contextKey, ToolDeployment toolDeployment);

    /**
     * Takes a real DB row lock (rather than an in-process-only lock) on this LTI context, held
     * for the caller's transaction, so concurrent roster syncs for the same context serialize
     * correctly even across multiple app instances - not just within one JVM.
     *
     * @param contextId
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM LtiContextEntity e WHERE e.contextId = :contextId")
    LtiContextEntity findByContextIdForUpdate(@Param("contextId") long contextId);

}
