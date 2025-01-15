package edu.iu.terracotta.connectors.generic.service.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.utils.lti.Lti3Request;
import org.springframework.transaction.annotation.Transactional;

public interface LtiDataService {

    @Transactional boolean loadLTIDataFromDB(Lti3Request lti, String link);
    @Transactional int upsertLTIDataInDB(Lti3Request lti, ToolDeployment toolDeployment, String link) throws DataServiceException;

    PlatformDeploymentRepository getPlatformDeploymentRepository();
    LtiUserEntity findByUserKeyAndPlatformDeployment(String userKey, PlatformDeployment platformDeployment);
    LtiUserEntity saveLtiUserEntity(LtiUserEntity ltiUserEntity);
    LtiMembershipEntity findByUserAndContext(LtiUserEntity ltiUserEntity, LtiContextEntity ltiContextEntity);
    LtiMembershipEntity saveLtiMembershipEntity(LtiMembershipEntity ltiMembershipEntity);
    ToolDeployment findOrCreateToolDeployment(String iss, String clientId, String ltiDeploymentId);
    String getOwnPrivateKey();
    void setOwnPrivateKey(String ownPrivateKey);
    String getOwnPublicKey();
    void setOwnPublicKey(String ownPublicKey);
    Boolean getDemoMode();
    void setDemoMode(Boolean demoMode);

}
