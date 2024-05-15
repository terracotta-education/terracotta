package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.PlatformDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface PlatformDeploymentRepository extends JpaRepository<PlatformDeployment, Long> {

    List<PlatformDeployment> findByIss(String iss);
    List<PlatformDeployment> findByClientId(String clientId);
    List<PlatformDeployment> findByToolDeployments_LtiDeploymentId(String ltiDeploymentId);
    List<PlatformDeployment> findByIssAndClientId(String iss, String clientId);
    List<PlatformDeployment> findByIssAndToolDeployments_LtiDeploymentId(String iss, String ltiDeploymentId);
    List<PlatformDeployment> findByIssAndClientIdAndToolDeployments_LtiDeploymentId(String iss, String clientId, String ltiDeploymentId);

}
