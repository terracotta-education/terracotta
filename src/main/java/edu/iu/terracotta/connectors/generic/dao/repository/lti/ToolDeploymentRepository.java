package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;

@Transactional
@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ToolDeploymentRepository extends JpaRepository<ToolDeployment, Long> {

    List<ToolDeployment> findByPlatformDeployment_Iss(String iss);
    List<ToolDeployment> findByPlatformDeployment_IssAndLtiDeploymentId(String iss, String ltiDeploymentId);
    List<ToolDeployment> findByPlatformDeployment_IssAndPlatformDeployment_ClientId(String iss, String clientId);
    List<ToolDeployment> findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(String iss, String clientId, String ltiDeploymentId);

}
