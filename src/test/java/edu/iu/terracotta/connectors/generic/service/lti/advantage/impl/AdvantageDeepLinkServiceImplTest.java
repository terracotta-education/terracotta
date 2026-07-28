package edu.iu.terracotta.connectors.generic.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiDeepLinkRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@SuppressWarnings({"unchecked"})
public class AdvantageDeepLinkServiceImplTest extends BaseTest {

    // not present in BaseRepositoryTest, so a plain local mock is unambiguous
    @Mock private LtiDeepLinkRepository ltiDeepLinkRepository;

    // named distinctly, and wired manually below: several other ConnectorService<?> mocks of
    // different generic parameterizations exist in BaseServiceTest, and since generics are
    // erased at runtime, relying on @InjectMocks to pick the right one would be ambiguous.
    @Mock private ConnectorService<AdvantageDeepLinkService> advantageDeepLinkConnectorService;

    @Mock private AdvantageDeepLinkService resolvedAdvantageDeepLinkService;

    private AdvantageDeepLinkServiceImpl advantageDeepLinkService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        advantageDeepLinkService = new AdvantageDeepLinkServiceImpl(
            ltiDeepLinkRepository,
            platformDeploymentRepository,
            advantageDeepLinkConnectorService,
            ltiJwtService
        );
    }

    @Test
    public void testGenerateDeepLinkJwtDelegatesToResolvedInstance() throws Exception {
        Jws<Claims> idToken = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(idToken.getPayload()).thenReturn(claims);
        when(claims.getIssuer()).thenReturn("issuer1");
        when(claims.getAudience()).thenReturn(Set.of("client1"));
        when(platformDeploymentRepository.findByIssAndClientId("issuer1", "client1")).thenReturn(List.of(platformDeployment));
        when(advantageDeepLinkConnectorService.instance(platformDeployment, AdvantageDeepLinkService.class)).thenReturn(resolvedAdvantageDeepLinkService);
        DeepLinkJwtDto dto = DeepLinkJwtDto.builder().jwt("jwt").returnUrl("https://example.com/return").build();
        when(resolvedAdvantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://example.com/return")).thenReturn(dto);

        DeepLinkJwtDto result = advantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://example.com/return");

        assertEquals(dto, result);
    }

    @Test
    public void testGenerateDeepLinkJwtThrowsWhenNoDeploymentFound() throws Exception {
        Jws<Claims> idToken = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(idToken.getPayload()).thenReturn(claims);
        when(claims.getIssuer()).thenReturn("issuer1");
        when(claims.getAudience()).thenReturn(Set.of("client1"));
        when(platformDeploymentRepository.findByIssAndClientId("issuer1", "client1")).thenReturn(Collections.emptyList());

        assertThrows(
            TerracottaConnectorException.class,
            () -> advantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://example.com/return")
        );
    }

    @Test
    public void testGenerateLtiDeepLinkBuildsAndSavesEntity() throws TerracottaConnectorException, GeneralSecurityException, IOException {
        Jws<Claims> stateClaims = mock(Jws.class);
        Claims claims = mock(Claims.class);
        when(ltiJwtService.validateState("state1")).thenReturn(stateClaims);
        when(stateClaims.getPayload()).thenReturn(claims);
        when(claims.getId()).thenReturn("nonce1");
        when(lti3Request.getDeepLinkReturnUrl()).thenReturn("https://example.com/return");
        when(lti3Request.getKey()).thenReturn(platformDeployment);
        when(ltiJwtService.generateTokenRequestJWT(platformDeployment)).thenReturn("token1");
        when(httpServletRequest.getParameter("id_token")).thenReturn("idTokenValue");
        when(ltiDeepLinkRepository.save(any(LtiDeepLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LtiDeepLink result = advantageDeepLinkService.generateLtiDeepLink(lti3Request, httpServletRequest, "state1");

        assertEquals("idTokenValue", result.getIdToken());
        assertEquals("nonce1", result.getNonce());
        assertEquals("https://example.com/return", result.getReturnUrl());
        assertEquals("state1", result.getState());
        assertEquals("token1", result.getToken());
    }

    @Test
    public void testFindByUuidReturnsEntityWhenFound() throws TerracottaConnectorException {
        UUID uuid = UUID.randomUUID();
        LtiDeepLink ltiDeepLink = LtiDeepLink.builder().state("state1").build();
        when(ltiDeepLinkRepository.findByUuid(uuid)).thenReturn(Optional.of(ltiDeepLink));

        LtiDeepLink result = advantageDeepLinkService.findByUuid(uuid);

        assertEquals(ltiDeepLink, result);
    }

    @Test
    public void testFindByUuidThrowsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(ltiDeepLinkRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThrows(TerracottaConnectorException.class, () -> advantageDeepLinkService.findByUuid(uuid));
    }

    @Test
    public void testDeleteDelegatesToRepository() {
        LtiDeepLink ltiDeepLink = LtiDeepLink.builder().state("state1").build();

        advantageDeepLinkService.delete(ltiDeepLink);

        verify(ltiDeepLinkRepository).delete(ltiDeepLink);
    }

}
