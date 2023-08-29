package edu.iu.terracotta.service.lti;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiMembershipEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.ToolDeployment;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.utils.lti.LTI3Request;
import org.springframework.transaction.annotation.Transactional;

public interface LTIDataService {

    @Transactional boolean loadLTIDataFromDB(LTI3Request lti, String link);
    @Transactional int upsertLTIDataInDB(LTI3Request lti, ToolDeployment toolDeployment, String link) throws DataServiceException;

    AllRepositories getAllRepositories();
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
