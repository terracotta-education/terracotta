package edu.iu.terracotta.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.ConsentDocument;


@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ConsentDocumentRepository extends JpaRepository<ConsentDocument, Long> {

    Optional<ConsentDocument> findByExperiment_ExperimentId(long experimentId);
    List<ConsentDocument> findAllByExperiment_PlatformDeployment_KeyId(long platformDeploymentId);

}
