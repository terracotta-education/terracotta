package edu.iu.terracotta.connectors.generic.service.lti;

import java.util.Collection;
import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.dao.repository.LtiNonceRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.utils.lti.Lti3Request;
import org.springframework.transaction.annotation.Transactional;

public interface LtiDataService {

    @Transactional boolean loadLTIDataFromDB(Lti3Request lti, String link);
    @Transactional int upsertLTIDataInDB(Lti3Request lti, ToolDeployment toolDeployment, String link) throws DataServiceException;

    // runs loadLTIDataFromDB and upsertLTIDataInDB in a single transaction, so the entities the
    // load resolves stay managed/attached for the upsert instead of needing to be re-merged
    @Transactional int loadAndUpsertLTIDataInDB(Lti3Request lti, ToolDeployment toolDeployment, String link) throws DataServiceException;

    PlatformDeploymentRepository getPlatformDeploymentRepository();
    LtiNonceRepository getLtiNonceRepository();
    LtiUserEntity findByUserKeyAndPlatformDeployment(String userKey, PlatformDeployment platformDeployment);
    // batches what would otherwise be one findByUserKeyAndPlatformDeployment call per user - e.g.
    // syncing a large course roster's new enrollees
    List<LtiUserEntity> findAllByUserKeysAndPlatformDeployment(Collection<String> userKeys, PlatformDeployment platformDeployment);
    LtiUserEntity saveLtiUserEntity(LtiUserEntity ltiUserEntity);
    LtiMembershipEntity findByUserAndContext(LtiUserEntity ltiUserEntity, LtiContextEntity ltiContextEntity);
    List<LtiMembershipEntity> findAllByUsersAndContext(Collection<LtiUserEntity> ltiUserEntities, LtiContextEntity ltiContextEntity);
    LtiMembershipEntity saveLtiMembershipEntity(LtiMembershipEntity ltiMembershipEntity);
    ToolDeployment findOrCreateToolDeployment(String iss, String clientId, String ltiDeploymentId);
    String getOwnPrivateKey();
    void setOwnPrivateKey(String ownPrivateKey);
    String getOwnPublicKey();
    void setOwnPublicKey(String ownPublicKey);
    Boolean getDemoMode();
    void setDemoMode(Boolean demoMode);

}
