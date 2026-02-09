package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;

public interface LtiDeepLinkRepository extends JpaRepository<LtiDeepLink, Long> {

    Optional<LtiDeepLink> findByUuid(UUID uuid);

}
