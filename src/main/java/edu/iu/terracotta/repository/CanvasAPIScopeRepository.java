package edu.iu.terracotta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.canvas.CanvasAPIScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface CanvasAPIScopeRepository extends JpaRepository<CanvasAPIScope, Long> {

    Optional<CanvasAPIScope> findByUuid(UUID uuid);
    List<CanvasAPIScope> findAllByFeatures_Id(long featureId);

}
