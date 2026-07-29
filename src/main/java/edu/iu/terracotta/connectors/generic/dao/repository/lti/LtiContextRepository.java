package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;

/**
 * NOTE: use of this interface magic makes all subclass-based (CGLIB) proxies fail
 */
@Transactional
public interface LtiContextRepository extends JpaRepository<LtiContextEntity, Long> {

    LtiContextEntity findByContextKey(String key);
    LtiContextEntity findByContextKeyAndToolDeployment(String contextKey, ToolDeployment toolDeployment);

    /**
     * Runs in its own transaction (independent of any caller's, which may be about to roll back)
     * so a failed roster sync's participant sync timestamp is reliably restored to its
     * pre-attempt value (which may itself be null) rather than left showing whatever partial
     * state existed at the moment of failure.
     *
     * @param contextId
     * @param lastParticipantSync
     */
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE LtiContextEntity e SET e.lastParticipantSync = :lastParticipantSync WHERE e.contextId = :contextId")
    void restoreLastParticipantSync(@Param("contextId") long contextId, @Param("lastParticipantSync") Instant lastParticipantSync);

}
