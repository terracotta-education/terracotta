package edu.iu.terracotta.repository.integrations;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.app.integrations.Integration;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    Optional<Integration> findByUuid(UUID uuid);
    boolean existsByUuidAndQuestion_QuestionId(UUID uuid, long questionId);

}
