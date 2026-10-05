package edu.iu.terracotta.connectors.brightspace.io.oauth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NonRefreshableOauthTokenTest {

    @Test
    public void testGetAccessTokenReturnsConstructorToken() {
        NonRefreshableOauthToken token = new NonRefreshableOauthToken("access-token-1");

        assertEquals("access-token-1", token.getAccessToken());
    }

    @Test
    public void testRefreshIsNoOp() {
        NonRefreshableOauthToken token = new NonRefreshableOauthToken("access-token-1");

        assertDoesNotThrow(token::refresh);
        assertEquals("access-token-1", token.getAccessToken());
    }

}
