package edu.iu.terracotta.controller.app.integrations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenNotFoundException;

public class IntegrationsControllerTest extends BaseTest {

    @InjectMocks private IntegrationsController integrationsController;

    public static final String returnQuery = "redirect:/app/app.html?integration=true&status=%s&preview=false&errorCode=%s&url=%s";

    @BeforeEach
    public void beforeEach() throws OutcomeNotMatchingException {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    void scoreTest() throws IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(StringUtils.contains(ret, "status=" + HttpStatus.OK.name()), ret);
    }

    @Test
    void scoreTokenNotFoundTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        doThrow(new IntegrationTokenNotFoundException("token")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(StringUtils.contains(ret, "status=" + HttpStatus.NOT_FOUND.name()), ret);
    }

    @Test
    void scoreBadRequestTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        doThrow(new IntegrationTokenInvalidException("token")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(StringUtils.contains(ret, "status=" + HttpStatus.BAD_REQUEST.name()), ret);
    }

    @Test
    void scoreExpiredBadRequestTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        doThrow(new IntegrationTokenExpiredException("token")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(StringUtils.contains(ret, "status=" + HttpStatus.BAD_REQUEST.name()), ret);
    }

    @Test
    void scoreAlreadyRedeemedBadRequestTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        doThrow(new IntegrationTokenAlreadyRedeemedException("token")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(StringUtils.contains(ret, "status=" + HttpStatus.BAD_REQUEST.name()), ret);
    }

}
