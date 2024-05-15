package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.ConsentDocument;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ConsentDocumentRepository extends JpaRepository<ConsentDocument, Long> {

    Optional<ConsentDocument> findByExperiment_ExperimentId(long experimentId);
    List<ConsentDocument> findAllByExperiment_PlatformDeployment_KeyId(long platformDeploymentId);

}
