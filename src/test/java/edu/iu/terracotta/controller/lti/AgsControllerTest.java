package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItems;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.utils.LtiStrings;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.servlet.http.HttpSession;

public class AgsControllerTest extends BaseTest {

    private AgsController agsController;

    private HttpSession session;
    private Model model;
    private Principal principal;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        // manual construction: AdvantageAgsService is also implemented by canvasAdvantageAgsService/
        // brightspaceAdvantageAgsService mocks in BaseServiceTest, so @InjectMocks constructor-injection
        // (type-only matching) could wire the wrong candidate.
        agsController = new AgsController(ltiContextRepository, toolDeploymentRepository, advantageAgsService);

        session = mock(HttpSession.class);
        model = mock(Model.class);
        principal = mock(Principal.class);
        when(httpServletRequest.getSession()).thenReturn(session);
    }

    private void mockSessionWithToolDeployment() {
        when(session.getAttribute(LtiStrings.LTI_SESSION_TOOL_DEPLOYMENT_ID)).thenReturn(1L);
        when(session.getAttribute(LtiStrings.LTI_SESSION_CONTEXT_ID)).thenReturn("context_key");
        when(toolDeploymentRepository.findById(anyLong())).thenReturn(Optional.of(toolDeployment));
        when(ltiContextRepository.findByContextKeyAndToolDeployment(anyString(), any(ToolDeployment.class))).thenReturn(ltiContextEntity);
    }

    @Test
    void agsGetLineItemsNoSessionValuesTest() throws ConnectionException, TerracottaConnectorException {
        // session attributes are null -> toolDeploymentRepository.findById(null) is unstubbed and
        // returns Optional.empty() by Mockito default, so the "present" branch never runs.
        String ret = agsController.agsGetLineItems(httpServletRequest, principal, model);

        assertEquals(AgsController.LTIADVAGSMAIN, ret);
        verify(model).addAttribute(TextConstants.NO_SESSION_VALUES, true);
        verify(model, never()).addAttribute(TextConstants.NO_SESSION_VALUES, false);
    }

    @Test
    void agsGetLineItemsHappyPathTest() throws ConnectionException, TerracottaConnectorException {
        mockSessionWithToolDeployment();

        String ret = agsController.agsGetLineItems(httpServletRequest, principal, model);

        assertEquals(AgsController.LTIADVAGSMAIN, ret);
        verify(model).addAttribute(LtiAgsScope.SINGLE.key(), false);
        verify(model).addAttribute(LtiAgsScope.RESULTS.key(), lineItems.getLineItemList());
    }

    @Test
    void agsGetLineItemsPropagatesConnectionExceptionTest() throws ConnectionException {
        mockSessionWithToolDeployment();
        doThrow(new ConnectionException("connection failed")).when(advantageAgsService).getToken(any(LtiAgsScope.class), any());

        assertThrows(ConnectionException.class, () -> agsController.agsGetLineItems(httpServletRequest, principal, model));
    }

    @Test
    void agsPostLineItemHappyPathTest() throws ConnectionException {
        mockSessionWithToolDeployment();
        when(advantageAgsService.postLineItems(any(), any(), any(LineItems.class))).thenReturn(lineItems);

        String ret = agsController.agsPostLineItem(httpServletRequest, principal, model, lineItems);

        assertEquals(AgsController.LTIADVAGSMAIN, ret);
        verify(model).addAttribute(LtiAgsScope.SINGLE.key(), false);
        verify(model).addAttribute(LtiAgsScope.RESULTS.key(), lineItems.getLineItemList());
    }

    @Test
    void agsGetLineitemHappyPathTest() throws ConnectionException {
        mockSessionWithToolDeployment();
        when(advantageAgsService.getLineItem(any(), any(), anyString())).thenReturn(lineItem);

        String ret = agsController.agsGetLineitem(httpServletRequest, principal, model, "1");

        assertEquals(AgsController.LTIADVAGSMAIN, ret);
        verify(model).addAttribute(LtiAgsScope.SINGLE.key(), true);
        verify(model).addAttribute(LtiAgsScope.RESULTS.key(), Collections.singletonList(lineItem));
    }

    @Test
    void agsPutLineitemHappyPathTest() throws ConnectionException {
        mockSessionWithToolDeployment();
        when(advantageAgsService.putLineItem(any(), any(), any(LineItem.class))).thenReturn(lineItem);

        String ret = agsController.agsPutLineitem(httpServletRequest, principal, model, lineItem, "1");

        assertEquals(AgsController.LTIADVAGSMAIN, ret);
        verify(lineItem).setId("1");
        verify(model).addAttribute(LtiAgsScope.SINGLE.key(), true);
    }

    @Test
    void agsPDeleteLineitemHappyPathTest() throws ConnectionException, TerracottaConnectorException {
        mockSessionWithToolDeployment();
        when(advantageAgsService.deleteLineItem(any(), any(), anyString())).thenReturn(true);

        String ret = agsController.agsPDeleteLineitem(httpServletRequest, principal, model, "1");

        assertEquals(AgsController.LTIADVAGSMAIN, ret);
        verify(model).addAttribute(LtiAgsScope.SINGLE.key(), false);
        verify(model).addAttribute(LtiAgsScope.RESULTS.key(), lineItems.getLineItemList());
        verify(model).addAttribute(LtiAgsScope.DELETE_RESULTS.key(), true);
    }

}
