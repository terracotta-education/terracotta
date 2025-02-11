package edu.iu.terracotta.connectors.generic.service.api.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiScope;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiScopeRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiScopeNotFoundException;
import edu.iu.terracotta.connectors.generic.service.api.ApiScopeService;
import edu.iu.terracotta.dao.model.enums.FeatureType;

@Service
public class ApiScopeServiceImpl implements ApiScopeService {

    @Autowired private ApiScopeRepository apiScopeRepository;

    @Override
    public ApiScope getScopeById(long id) throws ApiScopeNotFoundException {
        return apiScopeRepository.findById(id)
            .orElseThrow(
                () -> new ApiScopeNotFoundException(String.format("No API Scope found for ID: [%s] ", id))
            );
    }

    @Override
    public ApiScope getScopeByUUID(UUID uuid)  throws ApiScopeNotFoundException {
        return apiScopeRepository.findByUuid(uuid)
            .orElseThrow(
                () -> new ApiScopeNotFoundException(String.format("No API Scope found for UUID: [%s] ", uuid))
            );
    }

    @Override
    public ApiScope createScope(ApiScope apiScope) {
        return apiScopeRepository.save(apiScope);
    }

    @Override
    public ApiScope updateScope(ApiScope apiScope) {
        return apiScopeRepository.save(apiScope);
    }

    @Override
    public void deleteScope(Long id) {
        apiScopeRepository.deleteById(id);
    }

    @Override
    public List<String> getScopesForFeature(long featureId) {
        return apiScopeRepository.findAllByFeatures_Id(featureId)
            .stream()
            .map(ApiScope::getScope)
            .toList();
    }

    @Override
    public Set<String> getNecessaryScopes(long plaformDeploymentKeyId) {
        List<ApiScope> scopes = findScopesForPlatformDeploymentId(plaformDeploymentKeyId);
        Set<String> allNecessaryScopes = getDefaultScopes();

        for (ApiScope scope : scopes) {
            allNecessaryScopes.add(scope.getScope());
        }

        return allNecessaryScopes;
    }

    @Override
    public String getNecessaryScopes(long plaformDeploymentKeyId, String separator) {
        List<ApiScope> scopes = findScopesForPlatformDeploymentId(plaformDeploymentKeyId);
        Set<String> allNecessaryScopes = getDefaultScopes();

        for (ApiScope scope : scopes) {
            allNecessaryScopes.add(scope.getScope());
        }

        return StringUtils.join(allNecessaryScopes, separator);
    }

    private Set<String> getDefaultScopes() {
        List<ApiScope> scopes = apiScopeRepository.findAll();

        return scopes.stream()
            .filter(
                scope ->
                    scope.getFeatures().stream()
                        .anyMatch(feature -> feature.getType() == FeatureType.DEFAULT))
            .map(ApiScope::getScope)
            .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public List<ApiScope> getScopesForLmsConnector(LmsConnector lmsConnector) {
        return apiScopeRepository.findAll().stream()
            .filter(
                scope -> lmsConnector == scope.getLmsConnector()
            )
            .toList();
    }

    private List<ApiScope> findScopesForPlatformDeploymentId(long platformDeploymentKeyId) {
        return apiScopeRepository.findAll().stream()
        .filter(
            scope ->
                scope.getFeatures().stream()
                    .filter(
                        feature ->
                            feature.getPlatformDeployments().stream()
                                .filter(
                                    platformDeployment -> platformDeployment.getKeyId() == platformDeploymentKeyId
                                )
                                .toList()
                                .size() > 0
                    )
                    .toList()
                    .size() > 0
        )
        .toList();
    }

}
