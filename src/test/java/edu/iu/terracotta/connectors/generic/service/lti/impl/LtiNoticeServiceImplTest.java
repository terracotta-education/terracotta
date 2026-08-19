package edu.iu.terracotta.connectors.generic.service.lti.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ClaimsBuilder;
import io.jsonwebtoken.Jwts;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiMembershipEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.ToolDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.ToolDeploymentRepository;
import edu.iu.terracotta.utils.LtiStrings;

public class LtiNoticeServiceImplTest {

    private static final String ISS = "https://platform.example.com";
    private static final String CLIENT_ID = "client-id-value";
    private static final String DEPLOYMENT_ID = "deployment-1";
    private static final String CONTEXT_KEY = "canvas-course-42";
    private static final String USER_KEY = "instructor-user-key";

    @Mock private ToolDeploymentRepository toolDeploymentRepository;
    @Mock private LtiContextRepository ltiContextRepository;
    @Mock private LtiMembershipRepository ltiMembershipRepository;
    @Mock private PlatformDeployment platformDeployment;
    @Mock private ToolDeployment toolDeployment;
    @Mock private LtiContextEntity ltiContextEntity;
    @Mock private LtiMembershipEntity ltiMembershipEntity;
    @Mock private LtiUserEntity ltiUserEntity;

    private LtiNoticeServiceImpl ltiNoticeService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        ltiNoticeService = new LtiNoticeServiceImpl(toolDeploymentRepository, ltiContextRepository, ltiMembershipRepository);
    }

    private Claims noticeClaims(String iss, String clientId, String deploymentId, String contextKey) {
        ClaimsBuilder builder = Jwts.claims();

        if (iss != null) {
            builder.issuer(iss);
        }

        if (clientId != null) {
            builder.audience().add(clientId);
        }

        if (deploymentId != null) {
            builder.add(LtiStrings.LTI_DEPLOYMENT_ID, deploymentId);
        }

        if (contextKey != null) {
            builder.add(LtiStrings.LTI_CONTEXT, Map.of(LtiStrings.LTI_CONTEXT_ID, contextKey));
        }

        return builder.build();
    }

    @Test
    public void testResolveSecuredInfoSuccess() {
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(ISS, CLIENT_ID, DEPLOYMENT_ID))
            .thenReturn(List.of(toolDeployment));
        when(ltiContextRepository.findByContextKeyAndToolDeployment(CONTEXT_KEY, toolDeployment)).thenReturn(ltiContextEntity);
        when(ltiMembershipRepository.findFirstByContextAndRoleGreaterThanEqual(ltiContextEntity, 1)).thenReturn(Optional.of(ltiMembershipEntity));
        when(ltiMembershipEntity.getUser()).thenReturn(ltiUserEntity);
        when(ltiUserEntity.getUserKey()).thenReturn(USER_KEY);
        when(ltiContextEntity.getToolDeployment()).thenReturn(toolDeployment);
        when(toolDeployment.getPlatformDeployment()).thenReturn(platformDeployment);
        when(platformDeployment.getKeyId()).thenReturn(7L);
        when(ltiContextEntity.getContextId()).thenReturn(99L);
        when(ltiContextEntity.getContextKey()).thenReturn(CONTEXT_KEY);

        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(ISS, CLIENT_ID, DEPLOYMENT_ID, CONTEXT_KEY));

        assertTrue(result.isPresent());
        SecuredInfo securedInfo = result.get();
        assertEquals(7L, securedInfo.getPlatformDeploymentId());
        assertEquals(99L, securedInfo.getContextId());
        assertEquals(USER_KEY, securedInfo.getUserId());
        assertEquals(Roles.INSTRUCTOR_ROLE_LIST, securedInfo.getRoles());
        assertEquals(CONTEXT_KEY, securedInfo.getLmsCourseId());
    }

    @Test
    public void testResolveSecuredInfoMissingIssuerReturnsEmpty() {
        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(null, CLIENT_ID, DEPLOYMENT_ID, CONTEXT_KEY));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveSecuredInfoMissingAudienceReturnsEmpty() {
        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(ISS, null, DEPLOYMENT_ID, CONTEXT_KEY));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveSecuredInfoMissingDeploymentIdReturnsEmpty() {
        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(ISS, CLIENT_ID, null, CONTEXT_KEY));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveSecuredInfoMissingContextClaimReturnsEmpty() {
        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(ISS, CLIENT_ID, DEPLOYMENT_ID, null));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveSecuredInfoContextClaimNotAMapReturnsEmpty() {
        Claims claims = Jwts.claims()
            .issuer(ISS)
            .audience().add(CLIENT_ID).and()
            .add(LtiStrings.LTI_DEPLOYMENT_ID, DEPLOYMENT_ID)
            .add(LtiStrings.LTI_CONTEXT, "not-a-map")
            .build();

        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(claims);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveSecuredInfoNoToolDeploymentMatchReturnsEmpty() {
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(ISS, CLIENT_ID, DEPLOYMENT_ID))
            .thenReturn(List.of());

        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(ISS, CLIENT_ID, DEPLOYMENT_ID, CONTEXT_KEY));

        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveSecuredInfoNoContextMatchReturnsEmpty() {
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(ISS, CLIENT_ID, DEPLOYMENT_ID))
            .thenReturn(List.of(toolDeployment));
        when(ltiContextRepository.findByContextKeyAndToolDeployment(CONTEXT_KEY, toolDeployment)).thenReturn(null);

        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(ISS, CLIENT_ID, DEPLOYMENT_ID, CONTEXT_KEY));

        assertTrue(result.isEmpty());
    }

    // a course with no instructor on record (e.g. never launched) has no acting user to pick -
    // the caller is expected to skip the notice rather than fabricate an identity
    @Test
    public void testResolveSecuredInfoNoInstructorMembershipReturnsEmpty() {
        when(toolDeploymentRepository.findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(ISS, CLIENT_ID, DEPLOYMENT_ID))
            .thenReturn(List.of(toolDeployment));
        when(ltiContextRepository.findByContextKeyAndToolDeployment(CONTEXT_KEY, toolDeployment)).thenReturn(ltiContextEntity);
        when(ltiMembershipRepository.findFirstByContextAndRoleGreaterThanEqual(any(), any(Integer.class))).thenReturn(Optional.empty());

        Optional<SecuredInfo> result = ltiNoticeService.resolveSecuredInfo(noticeClaims(ISS, CLIENT_ID, DEPLOYMENT_ID, CONTEXT_KEY));

        assertTrue(result.isEmpty());
    }

}
