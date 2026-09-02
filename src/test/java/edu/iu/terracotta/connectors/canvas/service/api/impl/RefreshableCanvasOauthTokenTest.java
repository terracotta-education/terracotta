package edu.iu.terracotta.connectors.canvas.service.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.iu.terracotta.connectors.generic.exceptions.ApiException;

public class RefreshableCanvasOauthTokenTest {

    @Test
    public void testGetAccessTokenReturnsInitialValueBeforeRefresh() {
        RefreshableCanvasOauthToken token = new RefreshableCanvasOauthToken(rejected -> "new-token", "initial-token");

        assertEquals("initial-token", token.getAccessToken());
    }

    @Test
    public void testRefreshFetchesAndHoldsNewAccessToken() {
        RefreshableCanvasOauthToken token = new RefreshableCanvasOauthToken(rejected -> "new-token", "initial-token");

        token.refresh();

        assertEquals("new-token", token.getAccessToken());
    }

    @Test
    public void testRefreshPassesCurrentAccessTokenAsRejectedToken() {
        StringBuilder rejectedTokenSeen = new StringBuilder();
        RefreshableCanvasOauthToken token = new RefreshableCanvasOauthToken(
            rejected -> {
                rejectedTokenSeen.append(rejected);
                return "new-token";
            },
            "initial-token"
        );

        token.refresh();

        assertEquals("initial-token", rejectedTokenSeen.toString());
    }

    @Test
    public void testRefreshWrapsSupplierApiExceptionAsIllegalStateException() {
        RefreshableCanvasOauthToken token = new RefreshableCanvasOauthToken(
            rejected -> {
                throw new ApiException("could not fetch");
            },
            "initial-token"
        );

        IllegalStateException e = assertThrows(IllegalStateException.class, token::refresh);
        assertEquals(ApiException.class, e.getCause().getClass());
    }

}
