package edu.iu.terracotta.connectors.generic.dao.repository.lti;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiLinkEntity;

import java.util.UUID;
import java.util.List;

@Transactional
public interface LtiLinkRepository extends JpaRepository<LtiLinkEntity, Long> {

    LtiLinkEntity findByUuid(UUID uuid);

    List<LtiLinkEntity> findByLinkKey(String linkKey);
    List<LtiLinkEntity> findByLinkKeyAndContext(String linkKey, LtiContextEntity context);

}
