package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpSession;

public class MembershipControllerTest extends BaseTest {

    private static final long DEPLOYMENT_ID = 1L;
    private static final String CONTEXT_KEY = "context_key";

    @Mock private HttpSession httpSession;

    @InjectMocks private MembershipController membershipController;

    private Model model;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);
        setup();

        model = new ExtendedModelMap();

        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(toolDeploymentRepository.findById(any())).thenReturn(Optional.of(toolDeployment));
        when(ltiContextRepository.findByContextKeyAndToolDeployment(any(), any())).thenReturn(ltiContextEntity);
    }

    @Test
    void testMembershipGetPopulatesResults() throws Exception {
        when(httpSession.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID)).thenReturn(DEPLOYMENT_ID);
        when(httpSession.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID)).thenReturn(CONTEXT_KEY);
        when(advantageMembershipService.getAllLmsUsers(ltiToken, ltiContextEntity)).thenReturn(List.of());

        String view = membershipController.membershipGet(httpServletRequest, null, model);

        assertEquals("ltiAdvMembershipMain", view);
        assertEquals(List.of(), model.getAttribute(TextConstants.RESULTS));
    }

    @Test
    void testMembershipGetNoToolDeploymentInSession() throws Exception {
        when(httpSession.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID)).thenReturn(null);
        when(toolDeploymentRepository.findById(any())).thenReturn(Optional.empty());

        String view = membershipController.membershipGet(httpServletRequest, null, model);

        assertEquals("ltiAdvMembershipMain", view);
        assertNull(model.getAttribute(TextConstants.RESULTS));
        assertTrue((boolean) model.getAttribute(TextConstants.NO_SESSION_VALUES));
    }

    @Test
    void testMembershipGetToolDeploymentNotFound() throws Exception {
        when(httpSession.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID)).thenReturn(DEPLOYMENT_ID);
        when(httpSession.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID)).thenReturn(CONTEXT_KEY);
        when(toolDeploymentRepository.findById(DEPLOYMENT_ID)).thenReturn(Optional.empty());

        String view = membershipController.membershipGet(httpServletRequest, null, model);

        assertEquals("ltiAdvMembershipMain", view);
        assertNull(model.getAttribute(TextConstants.RESULTS));
    }

    @Test
    void testMembershipGetConnectionExceptionPropagates() throws Exception {
        when(httpSession.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID)).thenReturn(DEPLOYMENT_ID);
        when(httpSession.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID)).thenReturn(CONTEXT_KEY);
        when(advantageMembershipService.getToken(platformDeployment)).thenThrow(new ConnectionException("connection failed"));

        assertThrows(ConnectionException.class, () -> membershipController.membershipGet(httpServletRequest, null, model));
    }

}
