package edu.iu.terracotta.controller.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.FileInfoDto;
import edu.iu.terracotta.exceptions.BadConsentFileTypeException;
import edu.iu.terracotta.exceptions.BadTokenException;
import jakarta.servlet.ServletContext;

public class ConsentFileControllerTest extends BaseTest {

    private ConsentFileController consentFileController;

    @Mock private Resource resource;
    @Mock private ServletContext servletContext;

    @BeforeEach
    public void beforeEach() throws Exception {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiJwtService below also collides with CanvasApiJwtServiceImpl in BaseServiceTest (see the
        // @InjectMocks pitfall note there), so this class is constructed manually instead of relying
        // on @InjectMocks, which non-deterministically wired the wrong mock and left apiJwtService
        // calls silently unstubbed.
        consentFileController = new ConsentFileController(
            experimentRepository,
            apiJwtService,
            experimentService,
            fileStorageService
        );

        when(apiJwtService.extractValues(any(), eq(false))).thenReturn(securedInfo);
        when(httpServletRequest.getServletContext()).thenReturn(servletContext);
    }

    @Test
    void testPostConsentSuccess() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(multipartFile.getContentType()).thenReturn(MediaType.APPLICATION_PDF_VALUE);

        FileInfoDto fileInfoDto = FileInfoDto.builder().fileId("file-1").experimentId(1L).build();
        when(fileStorageService.uploadConsentFile(1L, "title", multipartFile, securedInfo)).thenReturn(fileInfoDto);

        ResponseEntity<FileInfoDto> response = consentFileController.postConsent(multipartFile, 1L, "title", httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(fileInfoDto, response.getBody());
    }

    @Test
    void testPostConsentUnauthorized() throws Exception {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<FileInfoDto> response = consentFileController.postConsent(multipartFile, 1L, "title", httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testPostConsentBadFileType() {
        when(apiJwtService.isInstructorOrHigher(securedInfo)).thenReturn(true);
        when(multipartFile.getContentType()).thenReturn(MediaType.TEXT_PLAIN_VALUE);

        assertThrows(BadConsentFileTypeException.class, () -> consentFileController.postConsent(multipartFile, 1L, "title", httpServletRequest));
    }

    @Test
    void testPostConsentPropagatesExperimentNotMatching() throws Exception {
        doThrow(new ExperimentNotMatchingException("not matching")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(ExperimentNotMatchingException.class, () -> consentFileController.postConsent(multipartFile, 1L, "title", httpServletRequest));
    }

    @Test
    void testGetConsentSuccess() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(fileStorageService.getConsentFile(1L)).thenReturn(resource);

        File realFile = File.createTempFile("consent", ".pdf");
        realFile.deleteOnExit();
        when(resource.getFile()).thenReturn(realFile);
        when(resource.getFilename()).thenReturn("consent.pdf");
        when(servletContext.getMimeType(anyString())).thenReturn(MediaType.APPLICATION_PDF_VALUE);

        ResponseEntity<Resource> response = consentFileController.getConsent(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("attachment; filename=\"consent.pdf\"; filename*=UTF-8''consent.pdf", response.getHeaders().getFirst("Content-Disposition"));
        assertEquals(resource, response.getBody());
    }

    @Test
    void testGetConsentContentTypeFallsBackOnIOException() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(fileStorageService.getConsentFile(1L)).thenReturn(resource);
        when(resource.getFile()).thenThrow(new IOException("boom"));
        when(resource.getFilename()).thenReturn("consent.pdf");

        ResponseEntity<Resource> response = consentFileController.getConsent(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
    }

    @Test
    void testGetConsentUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Resource> response = consentFileController.getConsent(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testGetConsentPropagatesBadToken() throws Exception {
        doThrow(new BadTokenException("bad token")).when(apiJwtService).experimentAllowed(securedInfo, 1L);

        assertThrows(BadTokenException.class, () -> consentFileController.getConsent(1L, httpServletRequest));
    }

    @Test
    void testDeleteConsentSuccessWithExperiment() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));

        ResponseEntity<Void> response = consentFileController.deleteConsent(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteConsentSuccessWithoutExperiment() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(true);
        when(experimentRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = consentFileController.deleteConsent(1L, httpServletRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testDeleteConsentUnauthorized() throws Exception {
        when(apiJwtService.isLearnerOrHigher(securedInfo)).thenReturn(false);

        ResponseEntity<Void> response = consentFileController.deleteConsent(1L, httpServletRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testDeleteConsentPropagatesTerracottaConnectorException() throws Exception {
        when(apiJwtService.extractValues(any(), eq(false))).thenThrow(new TerracottaConnectorException("connector down"));

        assertThrows(TerracottaConnectorException.class, () -> consentFileController.deleteConsent(1L, httpServletRequest));
    }

}
