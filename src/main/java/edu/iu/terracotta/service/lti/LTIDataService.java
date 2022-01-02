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
    AllRepositories getRepos();

    @Transactional
        //We check if we already have the information about this link in the database.
    boolean loadLTIDataFromDB(LTI3Request lti, String link);

    @Transactional
        // We update the information for the context, user, membership, link (if received), etc...  with new information on the LTI Request.
    int upsertLTIDataInDB(LTI3Request lti, ToolDeployment toolDeployment, String link) throws DataServiceException;

    LtiUserEntity findByUserKeyAndPlatformDeployment(String userKey, PlatformDeployment platformDeployment);

    LtiUserEntity saveLtiUserEntity(LtiUserEntity ltiUserEntity);

    LtiMembershipEntity findByUserAndContext(LtiUserEntity ltiUserEntity, LtiContextEntity ltiContextEntity);

    LtiMembershipEntity saveLtiMembershipEntity(LtiMembershipEntity ltiMembershipEntity);

    /**
     * Find or if possible create a ToolDeployment instance.
     * @param iss
     * @param clientId
     * @param ltiDeploymentId
     * @return {@link ToolDeployment} instance or null if not found and could not be created automatically
     */
    ToolDeployment findOrCreateToolDeployment(String iss, String clientId, String ltiDeploymentId);

    String getLocalUrl();

    void setLocalUrl(String localUrl);

    String getOwnPrivateKey();

    void setOwnPrivateKey(String ownPrivateKey);

    String getOwnPublicKey();

    void setOwnPublicKey(String ownPublicKey);

    Boolean getDemoMode();

    void setDemoMode(Boolean demoMode);
}
