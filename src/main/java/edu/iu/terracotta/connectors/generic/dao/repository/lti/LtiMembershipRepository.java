package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface LtiMembershipRepository extends JpaRepository<LtiMembershipEntity, Long> {

    LtiMembershipEntity findByUserAndContext(LtiUserEntity user, LtiContextEntity context);
    List<LtiMembershipEntity> findByUserInAndContext(Collection<LtiUserEntity> users, LtiContextEntity context);
    List<LtiMembershipEntity> findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(int role, long platformDeploymentId);
    // role is an ordinal scale (see Lti3Request.makeUserRoleNum: 0 = general/learner, 1 =
    // instructor, 2 = admin), so >= 1 covers instructor-or-higher - used to pick a real user to
    // act as an experiment's instructor when there's no live launch session to source one from
    // (e.g. an LTI Platform Notification Service notice)
    Optional<LtiMembershipEntity> findFirstByContextAndRoleGreaterThanEqual(LtiContextEntity context, int role);

}
