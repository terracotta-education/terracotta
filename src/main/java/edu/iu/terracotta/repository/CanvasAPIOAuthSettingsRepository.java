package edu.iu.terracotta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.canvas.CanvasAPIOAuthSettings;

public interface CanvasAPIOAuthSettingsRepository extends JpaRepository<CanvasAPIOAuthSettings, Long> {

    Optional<CanvasAPIOAuthSettings> findByPlatformDeployment(PlatformDeployment platformDeployment);

}
