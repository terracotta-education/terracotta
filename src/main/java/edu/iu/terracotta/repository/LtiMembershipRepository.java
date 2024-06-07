package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface LtiMembershipRepository extends JpaRepository<LtiMembershipEntity, Long> {

    LtiMembershipEntity findByUserAndContext(LtiUserEntity user, LtiContextEntity context);
    List<LtiMembershipEntity> findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(int role, long platformDeploymentId);

}
