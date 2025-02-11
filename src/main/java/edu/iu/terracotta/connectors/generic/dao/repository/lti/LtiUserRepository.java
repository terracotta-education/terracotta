package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface LtiUserRepository extends JpaRepository<LtiUserEntity, Long> {

    LtiUserEntity findByUserKeyAndPlatformDeployment(String linkKey, PlatformDeployment platformDeployment);
    LtiUserEntity findByUserKeyAndPlatformDeployment_KeyId(String userKey, long keyId);
    LtiUserEntity findByUserIdAndPlatformDeployment_KeyId(long userId, long keyId);
    LtiUserEntity findByUserId(long userId);

}
