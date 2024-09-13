package edu.iu.terracotta.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.app.Feature;
import edu.iu.terracotta.model.app.enumerator.FeatureType;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    Optional<Feature> findByType(FeatureType featureType);
    List<Feature> findAllByPlatformDeployments_KeyId(long platformDeploymentKeyId);

}
