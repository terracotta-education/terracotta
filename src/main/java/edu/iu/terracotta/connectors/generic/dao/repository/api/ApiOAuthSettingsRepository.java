package edu.iu.terracotta.connectors.generic.dao.repository.api;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOAuthSettings;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;

public interface ApiOAuthSettingsRepository extends JpaRepository<ApiOAuthSettings, Long> {

    Optional<ApiOAuthSettings> findByPlatformDeployment(PlatformDeployment platformDeployment);

}
