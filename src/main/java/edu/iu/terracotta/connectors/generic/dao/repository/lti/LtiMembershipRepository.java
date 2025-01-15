package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface LtiMembershipRepository extends JpaRepository<LtiMembershipEntity, Long> {

    LtiMembershipEntity findByUserAndContext(LtiUserEntity user, LtiContextEntity context);
    List<LtiMembershipEntity> findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(int role, long platformDeploymentId);

}
