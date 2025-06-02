package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface LtiUserRepository extends JpaRepository<LtiUserEntity, Long> {

    LtiUserEntity findFirstByUserKeyAndPlatformDeployment(String linkKey, PlatformDeployment platformDeployment);
    LtiUserEntity findFirstByUserKeyAndPlatformDeployment_KeyId(String userKey, long keyId);
    LtiUserEntity findFirstByUserIdAndPlatformDeployment_KeyId(long userId, long keyId);
    LtiUserEntity findFirstByEmailAndPlatformDeployment_KeyId(String email, long keyId);
    LtiUserEntity findFirstByUserId(long userId);
    Optional<LtiUserEntity> findFirstByLmsUserIdAndPlatformDeployment(String lmsUserId, PlatformDeployment platformDeployment);

}
