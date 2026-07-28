package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiDeepLink;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public class CanvasAdvantageDeepLinkServiceImplTest extends BaseTest {

    @InjectMocks private CanvasAdvantageDeepLinkServiceImpl canvasAdvantageDeepLinkService;

    @SuppressWarnings("unchecked")
    private final Jws<Claims> idToken = mock(Jws.class);

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();
    }

    @Test
    public void testGenerateDeepLinkJwt() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageDeepLinkService.generateDeepLinkJwt(List.of("id1"), idToken, "https://return.url")
        );
    }

    @Test
    public void testGenerateLtiDeepLink() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageDeepLinkService.generateLtiDeepLink(lti3Request, httpServletRequest, "state")
        );
    }

    @Test
    public void testFindByUuid() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageDeepLinkService.findByUuid(UUID.randomUUID())
        );
    }

    @Test
    public void testDelete() {
        LtiDeepLink ltiDeepLink = mock(LtiDeepLink.class);

        assertThrows(
            UnsupportedOperationException.class,
            () -> canvasAdvantageDeepLinkService.delete(ltiDeepLink)
        );
    }

}
