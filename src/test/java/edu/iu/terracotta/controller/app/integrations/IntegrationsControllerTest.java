package edu.iu.terracotta.controller.app.integrations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentLockedException;
import edu.iu.terracotta.exceptions.DataServiceException;

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

        assertTrue(Strings.CS.contains(ret, "status=" + HttpStatus.OK.name()), ret);
    }

    @Test
    void scoreTokenNotFoundTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException, IOException, AssignmentAttemptException, ApiException, TerracottaConnectorException, AssignmentLockedException {
        doThrow(new IntegrationTokenNotFoundException("{\"code\":\"token\",\"moreAttemptsAvailable\":true}")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));
        doNothing().when(questionSubmissionService).canSubmit(any(SecuredInfo.class), anyLong());
        when(integrationTokenLogRepository.findByCode(anyString())).thenReturn(Optional.empty());

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(Strings.CS.contains(ret, "status=" + HttpStatus.NOT_FOUND.name()), ret);
    }

    @Test
    void scoreBadRequestTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        doThrow(new IntegrationTokenInvalidException("{\"code\":\"token\",\"moreAttemptsAvailable\":true}")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(Strings.CS.contains(ret, "status=" + HttpStatus.BAD_REQUEST.name()), ret);
    }

    @Test
    void scoreExpiredBadRequestTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        doThrow(new IntegrationTokenExpiredException("{\"code\":\"token\",\"moreAttemptsAvailable\":true}")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(Strings.CS.contains(ret, "status=" + HttpStatus.BAD_REQUEST.name()), ret);
    }

    @Test
    void scoreAlreadyRedeemedBadRequestTest() throws IntegrationTokenNotFoundException, DataServiceException, IntegrationTokenInvalidException, IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        doThrow(new IntegrationTokenAlreadyRedeemedException("{\"code\":\"token\",\"moreAttemptsAvailable\":true}")).when(integrationScoreService).score("token", "1", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("token", "1", httpServletRequest);

        assertTrue(Strings.CS.contains(ret, "status=" + HttpStatus.BAD_REQUEST.name()), ret);
    }

    @Test
    void scoreEncodesWhatTheExternalSiteSent() throws Exception {
        doThrow(new IntegrationTokenNotFoundException("{\"code\":\"ortNu9\",\"moreAttemptsAvailable\":false}"))
            .when(integrationScoreService).score("${url_launch_token}", "1&url=https://example.com", Optional.of(INTEGRATION_CLIENT_NAME));

        String ret = integrationsController.score("${url_launch_token}", "1&url=https://example.com", httpServletRequest);

        assertTrue(Strings.CS.contains(ret, "launch_token=%24%7Burl_launch_token%7D&"), ret);
        assertTrue(Strings.CS.contains(ret, "score=1%26url%3Dhttps%3A%2F%2Fexample.com&"), ret);
        assertTrue(Strings.CS.contains(ret, "status=" + HttpStatus.NOT_FOUND.name()), ret);
        assertFalse(Strings.CS.contains(ret, "{"), ret);
    }

    @Test
    void scoreWithAnUnfilledPlaceholderRedirectsToTheErrorPage() throws Exception {
        // a site that sends its own placeholder unfilled: Spring used to throw "Model has no value for
        // key 'url_launch_token'" while redirecting, so the student got a server error, not the error code
        doThrow(new IntegrationTokenNotFoundException("{\"code\":\"ortNu9\",\"moreAttemptsAvailable\":false}"))
            .when(integrationScoreService).score(anyString(), any(), any());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(integrationsController).build();

        mockMvc.perform(get("/integrations").param("launch_token", "${url_launch_token}"))
            .andExpect(status().is3xxRedirection())
            .andExpect(result -> {
                String location = result.getResponse().getRedirectedUrl();
                assertTrue(Strings.CS.contains(location, "launch_token=%24%7Burl_launch_token%7D&"), location);
                assertTrue(Strings.CS.contains(location, "errorCode=ortNu9&"), location);
                assertTrue(Strings.CS.contains(location, "status=" + HttpStatus.NOT_FOUND.name()), location);
            });
    }

    @Test
    void previewTest() throws IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        String url = "aHR0cDovL2xvY2FsaG9zdA==";
        String ret = integrationsController.preview(url);

        assertTrue(Strings.CS.contains(ret, "previewUrl=" + url), ret);
    }

    @Test
    void testPreviewNoUrl() throws IntegrationTokenExpiredException, IntegrationTokenAlreadyRedeemedException {
        String ret = integrationsController.preview(null);

        assertTrue(Strings.CS.contains(ret, "status=" + HttpStatus.BAD_REQUEST.name()), ret);
    }

}
