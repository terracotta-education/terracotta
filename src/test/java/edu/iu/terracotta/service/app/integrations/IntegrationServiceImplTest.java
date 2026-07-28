package edu.iu.terracotta.service.app.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationConfigurationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationUrlIframeInvalidException;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationConfigurationDto;
import edu.iu.terracotta.dao.model.dto.integrations.IntegrationDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.impl.IntegrationServiceImpl;

public class IntegrationServiceImplTest extends BaseTest {

    @InjectMocks private IntegrationServiceImpl integrationService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        clearInvocations(
            integration,
            integrationRepository,
            integrationConfigurationService,
            question
        );
        setup();
    }

    @Test
    void testCreate() throws IntegrationClientNotFoundException {
        Integration ret = integrationService.create(question, UUID.randomUUID());

        assertNotNull(ret);
        verify(integrationRepository).save(any(Integration.class));
        verify(integrationConfigurationService).create(any(Integration.class), any(UUID.class));
    }

    @Test
    void testUpdate()
        throws IntegrationNotFoundException, IntegrationNotMatchingException, IntegrationConfigurationNotFoundException, IntegrationConfigurationNotMatchingException, IntegrationClientNotFoundException {
        Integration ret = integrationService.update(integrationDto, question);

        assertNotNull(ret);
        verify(integrationRepository).save(any(Integration.class));
        verify(integration).setConfiguration(any(IntegrationConfiguration.class));
        verify(integrationConfigurationService).update(any(IntegrationConfigurationDto.class), any(Integration.class));
    }

    @Test
    public void testUpdateIntegrationNotFoundException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(IntegrationNotFoundException.class, () -> { integrationService.update(integrationDto, question); });
    }

    @Test
    public void testUpdateIntegrationNotMatchingException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(question.getQuestionId()).thenReturn(1l, 2l);

        assertThrows(IntegrationNotMatchingException.class, () -> { integrationService.update(integrationDto, question); });
    }

    @Test
    void testDelete() {
        integrationService.delete(integration);

        verify(integrationConfigurationService).delete(any(IntegrationConfiguration.class));
        verify(integrationRepository).deleteById(anyLong());
    }

    @Test
    void testDeleteIntegrationNUll() {
        integrationService.delete(null);

        verify(integrationConfigurationService, never()).delete(any(IntegrationConfiguration.class));
        verify(integrationRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDuplicate() {
        integrationService.duplicate(integration, question);

        verify(integrationRepository).saveAndFlush(any(Integration.class));
        verify(integrationRepository).save(any(Integration.class));
        verify(integrationConfigurationService).duplicate(any(IntegrationConfiguration.class), any(Integration.class));
        verify(question).setIntegration(any(Integration.class));
    }

    @Test
    void testFindByUuid() throws IntegrationNotFoundException {
        Integration ret = integrationService.findByUuid(UUID.randomUUID());

        assertNotNull(ret);
    }

    @Test
    public void testFindByUuidIntegrationNotFoundException() throws IntegrationTokenInvalidException, DataServiceException, IntegrationTokenNotFoundException {
        when(integrationRepository.findByUuid(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(IntegrationNotFoundException.class, () -> { integrationService.findByUuid(any(UUID.class)); });
    }

    @Test
    void testToDtoList() {
        List<IntegrationDto> ret = integrationService.toDto(Collections.singletonList(integration));

        assertNotNull(ret);
        assertEquals(1, ret.size());
    }

    @Test
    void testToDtoListEmptyInput() {
        List<IntegrationDto> ret = integrationService.toDto(Collections.emptyList());

        assertNotNull(ret);
        assertEquals(0, ret.size());
    }

    @Test
    void testToDto() {
        IntegrationDto ret = integrationService.toDto(integration);

        assertNotNull(ret);
    }

    @Test
    void testFromDto() {
        Integration ret = integrationService.fromDto(integrationDto, integration);

        assertNotNull(ret);
    }

    @Test
    void testFromDtoDtoNull() {
        Integration ret = integrationService.fromDto(null, integration);

        assertNotNull(ret);
    }

    @Test
    void testFromDtoNull() {
        Integration ret = integrationService.fromDto(integrationDto, null);

        assertNotNull(ret);
    }

    @Test
    void testToDtoIntegrationNull() {
        IntegrationDto ret = integrationService.toDto((Integration) null);

        assertNull(ret);
    }

    @Test
    void testValidateIntegrationUrlIframePlatformDeploymentNotFound() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.empty());

        Exception exception = assertThrows(
            IntegrationUrlIframeInvalidException.class,
            () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
        );

        assertEquals("Platform deployment not found for iframe validation.", exception.getMessage());
    }

    @Test
    void testValidateIntegrationUrlIframeAllowedNoRestrictions() throws IntegrationUrlIframeInvalidException {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));

        try (MockedConstruction<RestTemplate> mocked = mockHeadResponse(new HttpHeaders())) {
            integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo);

            assertEquals(1, mocked.constructed().size());
        }
    }

    @Test
    void testValidateIntegrationUrlIframeXFrameOptionsDenyBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Frame-Options", "DENY");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            Exception exception = assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );

            assertTrue(exception.getMessage().contains("security headers"));
        }
    }

    @Test
    void testValidateIntegrationUrlIframeXFrameOptionsSameOriginMatchAllowed() throws IntegrationUrlIframeInvalidException {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        when(platformDeployment.getLocalUrl()).thenReturn("lti.url");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Frame-Options", "SAMEORIGIN");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            integrationService.validateIntegrationUrlIframe("https://lti.url/page", securedInfo);
        }
    }

    @Test
    void testValidateIntegrationUrlIframeXFrameOptionsSameOriginMismatchBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Frame-Options", "SAMEORIGIN");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            Exception exception = assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://other-domain.com", securedInfo); }
            );

            assertTrue(exception.getMessage().contains("security headers"));
        }
    }

    @Test
    void testValidateIntegrationUrlIframeXFrameOptionsSameOriginBlankDomainBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        when(platformDeployment.getLocalUrl()).thenReturn("");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Frame-Options", "SAMEORIGIN");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );
        }
    }

    @Test
    void testValidateIntegrationUrlIframeXFrameOptionsAllowFromMismatchBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Frame-Options", "ALLOW-FROM https://other.com");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );
        }
    }

    @Test
    void testValidateIntegrationUrlIframeXFrameOptionsUnknownValueBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Frame-Options", "WEIRD-VALUE");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );
        }
    }

    @Test
    void testValidateIntegrationUrlIframeCspNoneBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Security-Policy", "frame-ancestors 'none'");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            Exception exception = assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );

            assertTrue(exception.getMessage().contains("security headers"));
        }
    }

    @Test
    void testValidateIntegrationUrlIframeCspWildcardAllowed() throws IntegrationUrlIframeInvalidException {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Security-Policy", "frame-ancestors *");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo);
        }
    }

    @Test
    void testValidateIntegrationUrlIframeCspSelfBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Security-Policy", "frame-ancestors 'self'");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );
        }
    }

    @Test
    void testValidateIntegrationUrlIframeCspDomainInListAllowed() throws IntegrationUrlIframeInvalidException {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Security-Policy", "frame-ancestors http://lti.url https://other.com");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo);
        }
    }

    @Test
    void testValidateIntegrationUrlIframeCspDomainNotInListBlocked() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Security-Policy", "frame-ancestors https://other.com");

        try (MockedConstruction<RestTemplate> _ = mockHeadResponse(headers)) {
            Exception exception = assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );

            assertTrue(exception.getMessage().contains("security headers"));
        }
    }

    @Test
    void testValidateIntegrationUrlIframeHeadFailsGetSucceedsAllowFromMatchAllowed() throws IntegrationUrlIframeInvalidException {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Frame-Options", "ALLOW-FROM http://lti.url");
        ResponseEntity<String> response = new ResponseEntity<>(null, headers, HttpStatus.OK);

        try (
            MockedConstruction<RestTemplate> mocked = mockConstruction(
                RestTemplate.class,
                (mock, context) -> {
                    if (context.getCount() == 1) {
                        when(mock.exchange(anyString(), eq(HttpMethod.HEAD), isNull(), eq(String.class))).thenThrow(new RestTemplateTestException("head failed"));
                    } else {
                        when(mock.exchange(anyString(), eq(HttpMethod.GET), isNull(), eq(String.class))).thenReturn(response);
                    }
                }
            )
        ) {
            integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo);

            assertEquals(2, mocked.constructed().size());
        }
    }

    @Test
    void testValidateIntegrationUrlIframeHeadAndGetBothFail() {
        when(platformDeploymentRepository.findByKeyId(anyLong())).thenReturn(Optional.of(platformDeployment));

        try (
            MockedConstruction<RestTemplate> mocked = mockConstruction(
                RestTemplate.class,
                (mock, context) -> when(mock.exchange(anyString(), any(HttpMethod.class), isNull(), eq(String.class))).thenThrow(new RestTemplateTestException("failed"))
            )
        ) {
            Exception exception = assertThrows(
                IntegrationUrlIframeInvalidException.class,
                () -> { integrationService.validateIntegrationUrlIframe("https://example.com", securedInfo); }
            );

            assertEquals(2, mocked.constructed().size());
            assertTrue(exception.getMessage().contains("Error validating iframe embedding"));
        }
    }

    private MockedConstruction<RestTemplate> mockHeadResponse(HttpHeaders headers) {
        ResponseEntity<String> response = new ResponseEntity<>(null, headers, HttpStatus.OK);

        return mockConstruction(
            RestTemplate.class,
            (mock, context) -> when(mock.exchange(anyString(), eq(HttpMethod.HEAD), isNull(), eq(String.class))).thenReturn(response)
        );
    }

    private static class RestTemplateTestException extends RuntimeException {
        RestTemplateTestException(String message) {
            super(message);
        }
    }

}
