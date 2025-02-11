package edu.iu.terracotta.connectors.oneedtech.service.lms.impl;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.LmsOAuthException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsOAuthService;
import edu.iu.terracotta.dao.exceptions.FeatureNotFoundException;

@Service
@TerracottaConnector(LmsConnector.ONE_ED_TECH)
public class OneEdTechLmsOAuthServiceImpl implements LmsOAuthService<ApiTokenEntity> {

    @Override
    public boolean isConfigured(PlatformDeployment platformDeployment) {
        return false;
    }

    @Override
    public String getAuthorizationRequestURI(PlatformDeployment platformDeployment, String state) throws LmsOAuthException, FeatureNotFoundException {
        return null;
    }

    @Override
    public ApiTokenEntity fetchAndSaveAccessToken(LtiUserEntity user, String code) throws LmsOAuthException, FeatureNotFoundException {
        return null;
    }

    @Override
    public ApiTokenEntity getAccessToken(LtiUserEntity user) throws LmsOAuthException {
        return null;
    }

    @Override
    public boolean isAccessTokenAvailable(LtiUserEntity user) {
        return false;
    }

    @Override
    public RestTemplate createRestTemplate() {
        return new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }

}
