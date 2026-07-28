package edu.iu.terracotta.connectors.generic.dao.repository.api;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiScope;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.dao.model.enums.FeatureType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ApiScopeRepository extends JpaRepository<ApiScope, Long> {

    Optional<ApiScope> findByUuid(UUID uuid);
    List<ApiScope> findAllByFeatures_Id(long featureId);
    List<ApiScope> findAllByLmsConnector(LmsConnector lmsConnector);
    List<ApiScope> findAllByLmsConnectorAndFeatures_Type(LmsConnector lmsConnector, FeatureType featureType);
    List<ApiScope> findDistinctByFeatures_PlatformDeployments_KeyId(long platformDeploymentKeyId);

}
