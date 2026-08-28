package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.exceptions.BadTokenException;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TokenControllerTest extends BaseTest {

    private TokenController tokenController;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        // Constructed manually rather than via @InjectMocks: ApiJwtService is also implemented by the
        // inherited canvasApiJwtService mock (see the ambiguity warning in BaseServiceTest), so
        // constructor-injection-by-type could silently wire the wrong ApiJwtService mock.
        tokenController = new TokenController(apiJwtService);

        setup();
    }

    @Test
    void getTimedTokenTest() throws NumberFormatException, TerracottaConnectorException {
        ResponseEntity<String> expected = new ResponseEntity<>("timed-token", HttpStatus.OK);
        when(apiJwtService.getTimedToken(httpServletRequest)).thenReturn(expected);

        ResponseEntity<String> ret = tokenController.getTimedToken(httpServletRequest);

        assertEquals(expected, ret);
    }

    @Test
    void getTimedTokenPropagatesExceptionTest() throws NumberFormatException, TerracottaConnectorException {
        doThrow(new TerracottaConnectorException("error")).when(apiJwtService).getTimedToken(httpServletRequest);

        assertThrows(TerracottaConnectorException.class, () -> tokenController.getTimedToken(httpServletRequest));
    }

    @Test
    void refreshTokenOkTest() throws GeneralSecurityException, IOException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        when(apiJwtService.extractJwtStringValue(httpServletRequest, true)).thenReturn("jwt-token");
        when(apiJwtService.refreshToken("jwt-token")).thenReturn("new-refresh-token");

        ResponseEntity ret = tokenController.refreshToken(httpServletRequest);

        assertEquals(HttpStatus.OK, ret.getStatusCode());
        assertEquals("new-refresh-token", ret.getBody());
    }

    @Test
    void refreshTokenBlankResultBadRequestTest() throws GeneralSecurityException, IOException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        when(apiJwtService.extractJwtStringValue(httpServletRequest, true)).thenReturn("jwt-token");
        when(apiJwtService.refreshToken("jwt-token")).thenReturn("");

        ResponseEntity ret = tokenController.refreshToken(httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
        assertEquals("Error generating token", ret.getBody());
    }

    @Test
    void refreshTokenGeneralSecurityExceptionTest() throws GeneralSecurityException, IOException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        when(apiJwtService.extractJwtStringValue(httpServletRequest, true)).thenReturn("jwt-token");
        doThrow(new GeneralSecurityException("bad key")).when(apiJwtService).refreshToken("jwt-token");

        ResponseEntity ret = tokenController.refreshToken(httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ret.getStatusCode());
        assertEquals("Error generating token", ret.getBody());
    }

    @Test
    void refreshTokenIOExceptionTest() throws GeneralSecurityException, IOException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        when(apiJwtService.extractJwtStringValue(httpServletRequest, true)).thenReturn("jwt-token");
        doThrow(new IOException("io error")).when(apiJwtService).refreshToken("jwt-token");

        ResponseEntity ret = tokenController.refreshToken(httpServletRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ret.getStatusCode());
        assertEquals("Error generating token", ret.getBody());
    }

    @Test
    void refreshTokenOtherExceptionBadRequestTest() throws GeneralSecurityException, IOException, BadTokenException, NumberFormatException, TerracottaConnectorException {
        when(apiJwtService.extractJwtStringValue(httpServletRequest, true)).thenReturn("jwt-token");
        doThrow(new BadTokenException("bad token")).when(apiJwtService).refreshToken("jwt-token");

        ResponseEntity ret = tokenController.refreshToken(httpServletRequest);

        assertEquals(HttpStatus.BAD_REQUEST, ret.getStatusCode());
        assertEquals("bad token", ret.getBody());
    }

}
