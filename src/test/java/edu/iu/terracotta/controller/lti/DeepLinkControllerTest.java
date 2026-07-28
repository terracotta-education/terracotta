package edu.iu.terracotta.controller.lti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.GeneralSecurityException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.DeepLinkJwtDto;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageDeepLinkService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@SuppressWarnings("unchecked")
public class DeepLinkControllerTest extends BaseTest {

    @Mock private AdvantageDeepLinkService deepLinkService;

    @InjectMocks private DeepLinkController deepLinkController;

    private final Jws<Claims> stateClaimsJws = mock(Jws.class);
    private final Jws<Claims> idTokenJws = mock(Jws.class);
    private final Claims stateClaims = mock(Claims.class);

    private LtiDeepLink ltiDeepLink;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        ltiDeepLink = LtiDeepLink.builder()
            .state("state")
            .idToken("idToken")
            .returnUrl("http://return.url")
            .build();
    }

    @Test
    void deepLinksToJwtTest() throws Exception {
        when(deepLinkService.findByUuid(any(UUID.class))).thenReturn(ltiDeepLink);
        when(ltiJwtService.validateState("state")).thenReturn(stateClaimsJws);
        when(stateClaimsJws.getPayload()).thenReturn(stateClaims);
        when(stateClaims.get("clientId", String.class)).thenReturn("clientId");
        when(ltiJwtService.validateJWT("idToken", "clientId")).thenReturn(idTokenJws);
        DeepLinkJwtDto dto = DeepLinkJwtDto.builder().jwt("jwt").returnUrl("http://return.url").build();
        when(deepLinkService.generateDeepLinkJwt(anyList(), eq(idTokenJws), eq("http://return.url"))).thenReturn(dto);

        ResponseEntity<Object> response = deepLinkController.deepLinksToJwt(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void deepLinksToJwtDeleteFailureStillReturnsOkTest() throws Exception {
        when(deepLinkService.findByUuid(any(UUID.class))).thenReturn(ltiDeepLink);
        when(ltiJwtService.validateState("state")).thenReturn(stateClaimsJws);
        when(stateClaimsJws.getPayload()).thenReturn(stateClaims);
        when(stateClaims.get("clientId", String.class)).thenReturn("clientId");
        when(ltiJwtService.validateJWT("idToken", "clientId")).thenReturn(idTokenJws);
        DeepLinkJwtDto dto = DeepLinkJwtDto.builder().jwt("jwt").returnUrl("http://return.url").build();
        when(deepLinkService.generateDeepLinkJwt(anyList(), eq(idTokenJws), eq("http://return.url"))).thenReturn(dto);
        doThrow(new RuntimeException("delete failed")).when(deepLinkService).delete(ltiDeepLink);

        ResponseEntity<Object> response = deepLinkController.deepLinksToJwt(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void deepLinksToJwtFindByUuidBadRequestTest() throws Exception {
        when(deepLinkService.findByUuid(any(UUID.class))).thenThrow(new TerracottaConnectorException("not found"));

        ResponseEntity<Object> response = deepLinkController.deepLinksToJwt(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deepLinksToJwtGenerateJwtBadRequestTest() throws Exception {
        when(deepLinkService.findByUuid(any(UUID.class))).thenReturn(ltiDeepLink);
        when(ltiJwtService.validateState("state")).thenReturn(stateClaimsJws);
        when(stateClaimsJws.getPayload()).thenReturn(stateClaims);
        when(stateClaims.get("clientId", String.class)).thenReturn("clientId");
        when(ltiJwtService.validateJWT("idToken", "clientId")).thenReturn(idTokenJws);
        when(deepLinkService.generateDeepLinkJwt(anyList(), eq(idTokenJws), eq("http://return.url"))).thenThrow(new GeneralSecurityException("bad key"));

        ResponseEntity<Object> response = deepLinkController.deepLinksToJwt(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deepLinksToJwtInternalServerErrorTest() throws Exception {
        when(deepLinkService.findByUuid(any(UUID.class))).thenReturn(ltiDeepLink);
        when(ltiJwtService.validateState("state")).thenThrow(new RuntimeException("unexpected"));

        ResponseEntity<Object> response = deepLinkController.deepLinksToJwt(UUID.randomUUID(), httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

}
