package edu.iu.terracotta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.canvas.CanvasAPITokenEntity;

public interface CanvasAPITokenRepository extends JpaRepository<CanvasAPITokenEntity, Long> {

    CanvasAPITokenEntity findByUser(LtiUserEntity user);
}
