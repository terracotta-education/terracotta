package edu.iu.terracotta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;

public interface CanvasAPITokenRepository extends JpaRepository<CanvasAPITokenEntity, Long> {

    Optional<CanvasAPITokenEntity> findByUser(LtiUserEntity user);

}
