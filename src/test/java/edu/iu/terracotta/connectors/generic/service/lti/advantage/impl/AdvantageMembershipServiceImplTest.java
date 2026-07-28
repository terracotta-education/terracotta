package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageMembershipService;

public class AdvantageMembershipServiceImplTest extends BaseTest {

    // named distinctly, and wired manually below: several other ConnectorService<?> mocks of
    // different generic parameterizations exist in BaseServiceTest, and since generics are
    // erased at runtime, relying on @InjectMocks to pick the right one would be ambiguous.
    @Mock private ConnectorService<AdvantageMembershipService> advantageMembershipConnectorService;

    // the resolved per-connector instance returned by the connector service - kept distinct from
    // the inherited `advantageMembershipService` interface mock (which is shadowed below by the
    // concrete class under test, matching this codebase's established @InjectMocks convention).
    @Mock private AdvantageMembershipService resolvedAdvantageMembershipService;

    private AdvantageMembershipServiceImpl advantageMembershipService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        advantageMembershipService = new AdvantageMembershipServiceImpl(advantageMembershipConnectorService);
    }

    @Test
    public void testGetTokenDelegatesToResolvedInstanceByPlatformDeployment() throws ConnectionException, TerracottaConnectorException {
        when(advantageMembershipConnectorService.instance(platformDeployment, AdvantageMembershipService.class)).thenReturn(resolvedAdvantageMembershipService);
        when(resolvedAdvantageMembershipService.getToken(platformDeployment)).thenReturn(ltiToken);

        LtiToken result = advantageMembershipService.getToken(platformDeployment);

        assertEquals(ltiToken, result);
    }

    @Test
    public void testCallMembershipServiceDelegatesThroughLtiContextsToolDeployment() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();
        // ltiContextEntity -> toolDeployment -> platformDeployment is pre-wired by BaseModelTest
        when(advantageMembershipConnectorService.instance(platformDeployment, AdvantageMembershipService.class)).thenReturn(resolvedAdvantageMembershipService);
        when(resolvedAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId)).thenReturn(courseUsers);

        CourseUsers result = advantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId);

        assertEquals(courseUsers, result);
    }

    @Test
    public void testCallMembershipServiceOnlyStudentsDelegatesThroughLtiContextsToolDeployment() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();
        when(advantageMembershipConnectorService.instance(platformDeployment, AdvantageMembershipService.class)).thenReturn(resolvedAdvantageMembershipService);
        when(resolvedAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true)).thenReturn(courseUsers);

        CourseUsers result = advantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId, true);

        assertEquals(courseUsers, result);
    }

    @Test
    public void testGetAllLmsUsersDelegatesThroughLtiContextsToolDeployment() throws ConnectionException, TerracottaConnectorException {
        LmsUserBatch lmsUserBatch = org.mockito.Mockito.mock(LmsUserBatch.class);
        when(advantageMembershipConnectorService.instance(platformDeployment, AdvantageMembershipService.class)).thenReturn(resolvedAdvantageMembershipService);
        when(resolvedAdvantageMembershipService.getAllLmsUsers(ltiToken, ltiContextEntity)).thenReturn(List.of(lmsUserBatch));

        List<LmsUserBatch> result = advantageMembershipService.getAllLmsUsers(ltiToken, ltiContextEntity);

        assertEquals(List.of(lmsUserBatch), result);
    }

    @Test
    public void testGetTokenPropagatesConnectorException() throws ConnectionException, TerracottaConnectorException {
        when(advantageMembershipConnectorService.instance(platformDeployment, AdvantageMembershipService.class)).thenThrow(new TerracottaConnectorException("boom"));

        assertThrows(TerracottaConnectorException.class, () -> advantageMembershipService.getToken(platformDeployment));
    }

    @Test
    public void testCallMembershipServicePropagatesConnectionException() throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();
        when(advantageMembershipConnectorService.instance(platformDeployment, AdvantageMembershipService.class)).thenReturn(resolvedAdvantageMembershipService);
        when(resolvedAdvantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId)).thenThrow(new ConnectionException("connection failed"));

        assertThrows(ConnectionException.class, () -> advantageMembershipService.callMembershipService(ltiToken, ltiContextEntity, batchId));
    }

}
