package edu.iu.terracotta.connectors.generic.dao.repository.api;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ApiTokenRepository extends JpaRepository<ApiTokenEntity, Long> {

    Optional<ApiTokenEntity> findByUser(LtiUserEntity user);
    List<ApiTokenEntity> findAllByLmsConnector(LmsConnector lmsConnector);

}
