package edu.iu.terracotta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.canvas.CanvasAPIScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface CanvasAPIScopeRepository extends JpaRepository<CanvasAPIScope, Long> {

    List<CanvasAPIScope> findByRequired(boolean required);
    Optional<CanvasAPIScope> findByUuid(UUID uuid);

}
