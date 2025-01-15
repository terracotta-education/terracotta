package edu.iu.terracotta.connectors.generic.service.lti.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lti.dto.ToolRegistrationDto;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@SuppressWarnings({"unchecked"})
public class RegistrationServiceImplTest extends BaseTest {

    @Spy
    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private ToolRegistrationDto toolRegistrationDto;
    private String token;
    private String endpoint;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        doReturn(restTemplate).when(registrationService).createRestTemplate();

        toolRegistrationDto = new ToolRegistrationDto();
        token = "testToken";
        endpoint = "https://example.com/registration";
    }

    @Test
    public void testCallDynamicRegistrationSuccess() throws ConnectionException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Success", HttpStatusCode.valueOf(200));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(responseEntity);

        String ret = registrationService.callDynamicRegistration(token, toolRegistrationDto, endpoint);

        assertEquals("Success", ret);
    }

    @Test
    public void testCallDynamicRegistrationFailure() throws ConnectionException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Failure", HttpStatusCode.valueOf(400));

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(responseEntity);

        assertThrows(ConnectionException.class, () -> registrationService.callDynamicRegistration(token, toolRegistrationDto, endpoint));
    }

    @Test
    public void testCallDynamicRegistrationException() throws ConnectionException {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenThrow(new RuntimeException("Error"));
        when(exceptionMessageGenerator.exceptionMessage(anyString(), any(Exception.class))).thenReturn("Generated exception message");

        ConnectionException exception = assertThrows(ConnectionException.class, () -> registrationService.callDynamicRegistration(token, toolRegistrationDto, endpoint));

        assertEquals("Generated exception message", exception.getMessage());
        verify(exceptionMessageGenerator).exceptionMessage(anyString(), any(Exception.class));
    }
}