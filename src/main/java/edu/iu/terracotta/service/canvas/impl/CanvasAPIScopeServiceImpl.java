package edu.iu.terracotta.service.canvas.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.CanvasAPIScopeNotFoundException;
import edu.iu.terracotta.exceptions.app.FeatureNotFoundException;
import edu.iu.terracotta.model.app.Feature;
import edu.iu.terracotta.model.app.enumerator.FeatureType;
import edu.iu.terracotta.model.canvas.CanvasAPIScope;
import edu.iu.terracotta.repository.CanvasAPIScopeRepository;
import edu.iu.terracotta.repository.FeatureRepository;
import edu.iu.terracotta.service.canvas.CanvasAPIScopeService;

@Service
public class CanvasAPIScopeServiceImpl implements CanvasAPIScopeService {

    @Autowired private CanvasAPIScopeRepository canvasAPIScopeRepository;
    @Autowired private FeatureRepository featureRepository;

    @Override
    public CanvasAPIScope getScopeById(long id) throws CanvasAPIScopeNotFoundException {
        return canvasAPIScopeRepository
            .findById(id)
            .orElseThrow(
                () -> new CanvasAPIScopeNotFoundException(String.format("No Canvas API Scope found for ID: [%s] ", id))
            );
    }

    @Override
    public CanvasAPIScope getScopeByUUID(UUID uuid)  throws CanvasAPIScopeNotFoundException {
        return canvasAPIScopeRepository
            .findByUuid(uuid)
            .orElseThrow(
                () -> new CanvasAPIScopeNotFoundException(String.format("No Canvas API Scope found for UUID: [%s] ", uuid))
            );
    }

    @Override
    public CanvasAPIScope createScope(CanvasAPIScope canvasAPIScope) {
        return canvasAPIScopeRepository.save(canvasAPIScope);
    }

    @Override
    public CanvasAPIScope updateScope(CanvasAPIScope canvasAPIScope) {
        return canvasAPIScopeRepository.save(canvasAPIScope);
    }

    @Override
    public void deleteScope(Long id) {
        canvasAPIScopeRepository.deleteById(id);
    }

    @Override
    public List<String> getScopesForFeature(long featureId) {
        return canvasAPIScopeRepository.findAllByFeatures_Id(featureId)
            .stream()
            .map(CanvasAPIScope::getScope)
            .toList();
    }

    @Override
    public Set<String> getNecessaryScopes(long plaformDeploymentKeyId) throws FeatureNotFoundException {
        List<Feature> features = featureRepository.findAllByPlatformDeployments_KeyId(plaformDeploymentKeyId);
        Set<String> allNecessaryScopes = getDefaultScopes();

        for (Feature feature : features) {
            for (CanvasAPIScope scope : feature.getScopes()) {
                allNecessaryScopes.add(scope.getScope());
            }
        }

        return allNecessaryScopes;
    }

    @Override
    public String getNecessaryScopes(long plaformDeploymentKeyId, String separator) throws FeatureNotFoundException {
        List<Feature> features = featureRepository.findAllByPlatformDeployments_KeyId(plaformDeploymentKeyId);
        Set<String> allNecessaryScopes = getDefaultScopes();

        for (Feature feature : features) {
            for (CanvasAPIScope scope : feature.getScopes()) {
                allNecessaryScopes.add(scope.getScope());
            }
        }

        return StringUtils.join(allNecessaryScopes, separator);
    }

    private Set<String> getDefaultScopes() throws FeatureNotFoundException {
        Feature defaultFeature = featureRepository.findByType(FeatureType.DEFAULT)
            .orElseThrow(() -> new FeatureNotFoundException(String.format("No feature found for type: [%s]", FeatureType.DEFAULT)));

        return defaultFeature.getScopes().stream()
            .map(CanvasAPIScope::getScope)
            .collect(Collectors.toCollection(HashSet::new));
    }

}
