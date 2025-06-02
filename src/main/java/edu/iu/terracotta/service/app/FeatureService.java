package edu.iu.terracotta.service.app;

import java.util.List;

import edu.iu.terracotta.dao.entity.Feature;
import edu.iu.terracotta.dao.model.dto.FeatureDto;
import edu.iu.terracotta.dao.model.enums.FeatureType;


public interface FeatureService {

    FeatureDto toDto(Feature feature);
    List<FeatureDto> toDto(List<Feature> features);
    boolean isFeatureEnabled(FeatureType featureType, long platformDeploymentKeyId);

}
