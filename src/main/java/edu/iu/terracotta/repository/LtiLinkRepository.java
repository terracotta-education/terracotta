package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface LtiLinkRepository extends JpaRepository<LtiLinkEntity, Long> {

    List<LtiLinkEntity> findByLinkKey(String linkKey);
    List<LtiLinkEntity> findByLinkKeyAndContext(String linkKey, LtiContextEntity context);

}
