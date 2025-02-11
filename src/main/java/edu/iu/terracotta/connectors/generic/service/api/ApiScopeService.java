package edu.iu.terracotta.connectors.generic.service.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiScope;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.ApiScopeNotFoundException;

public interface ApiScopeService {

    ApiScope getScopeById(long id) throws ApiScopeNotFoundException;
    ApiScope getScopeByUUID(UUID uuid) throws ApiScopeNotFoundException;
    ApiScope createScope(ApiScope canvasAPIScope);
    ApiScope updateScope(ApiScope canvasAPIScope);
    void deleteScope(Long id);
    List<String> getScopesForFeature(long featureId);
    String getNecessaryScopes(long plaformDeploymentKeyId, String separator);
    Set<String> getNecessaryScopes(long plaformDeploymentKeyId);
    List<ApiScope> getScopesForLmsConnector(LmsConnector lmsConnector);

}
