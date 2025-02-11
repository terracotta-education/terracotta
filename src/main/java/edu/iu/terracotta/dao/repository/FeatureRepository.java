package edu.iu.terracotta.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.Feature;
import edu.iu.terracotta.dao.model.enums.FeatureType;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    Optional<Feature> findByType(FeatureType featureType);
    List<Feature> findAllByPlatformDeployments_KeyId(long platformDeploymentKeyId);

}
