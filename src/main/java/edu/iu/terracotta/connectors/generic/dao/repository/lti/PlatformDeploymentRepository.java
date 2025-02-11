package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;

import java.util.List;
import java.util.Optional;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface PlatformDeploymentRepository extends JpaRepository<PlatformDeployment, Long> {

    Optional<PlatformDeployment> findByKeyId(long keyId);
    List<PlatformDeployment> findByIss(String iss);
    List<PlatformDeployment> findByClientId(String clientId);
    List<PlatformDeployment> findByToolDeployments_LtiDeploymentId(String ltiDeploymentId);
    List<PlatformDeployment> findByIssAndClientId(String iss, String clientId);
    List<PlatformDeployment> findByIssAndToolDeployments_LtiDeploymentId(String iss, String ltiDeploymentId);
    List<PlatformDeployment> findByIssAndClientIdAndToolDeployments_LtiDeploymentId(String iss, String clientId, String ltiDeploymentId);
    List<PlatformDeployment> findAllByLmsConnector(LmsConnector lmsConnector);

}
