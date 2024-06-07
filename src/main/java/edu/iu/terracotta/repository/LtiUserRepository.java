package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface LtiUserRepository extends JpaRepository<LtiUserEntity, Long> {

    LtiUserEntity findByUserKeyAndPlatformDeployment(String linkKey, PlatformDeployment platformDeployment);
    LtiUserEntity findByUserKeyAndPlatformDeployment_KeyId(String userKey, long keyId);
    LtiUserEntity findByUserIdAndPlatformDeployment_KeyId(long userId, long keyId);
    LtiUserEntity findByUserId(long userId);

}
