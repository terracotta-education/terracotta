package edu.iu.terracotta.service.app.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.Feature;
import edu.iu.terracotta.dao.model.dto.FeatureDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;
import edu.iu.terracotta.dao.repository.FeatureRepository;
import edu.iu.terracotta.service.app.FeatureService;

@Service
@SuppressWarnings({"PMD.LambdaCanBeMethodReference"})
public class FeatureServiceImpl implements FeatureService {

    @Autowired private FeatureRepository featureRepository;

    @Override
    public List<FeatureDto> toDto(List<Feature> features) {
        return features.stream()
            .map(feature -> toDto(feature))
            .toList();
    }

    @Override
    public FeatureDto toDto(Feature feature) {
        return FeatureDto.builder()
            .type(feature.getType())
            .build();
    }

    @Override
    public boolean isFeatureEnabled(FeatureType featureType, long platformDeploymentKeyId) {
        return featureRepository.findAllByPlatformDeployments_KeyId(platformDeploymentKeyId).stream()
            .anyMatch(feature -> featureType == feature.getType());
    }

}
