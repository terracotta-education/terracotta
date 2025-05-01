package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

class ExperimentImportServiceImplTest extends BaseTest {

    @InjectMocks private ExperimentImportServiceImpl experimentImportService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
    }

    /*@Test
    void testPreprocessSuccess() throws ExperimentImportException, IOException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId("user-id", 1L)).thenReturn(ltiUserEntity);
        when(ltiContextRepository.findById(1L)).thenReturn(Optional.of(ltiContextEntity));
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.zip");
        when(experimentImportRepository.save(any(ExperimentImport.class))).thenReturn(experimentImport);
        when(experimentImport.getErrorMessages()).thenReturn(Collections.emptyList());
        when(FileUtils.getFile(any(File.class), anyString())).thenReturn(file);

        ImportDto result = experimentImportService.preprocess(multipartFile, securedInfo);

        assertNotNull(result);
        verify(fileStorageService).saveExperimentImportFile(eq(multipartFile), any(ExperimentImport.class));
        verify(experimentImportAsyncService).process(any(ExperimentImport.class), eq(securedInfo));
    }*/

    @Test
    void testPreprocessContextNotFound() {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(ltiContextRepository.findById(1L)).thenReturn(Optional.empty());

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> {
            experimentImportService.preprocess(multipartFile, securedInfo);
        });

        assertEquals("Context ID: [1] not found", exception.getMessage());
    }

    /*@Test
    void testPreprocessValidationError() throws IOException, ExperimentImportException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId("user-id", 1L)).thenReturn(ltiUserEntity);
        when(ltiContextRepository.findById(1L)).thenReturn(Optional.of(ltiContextEntity));
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.zip");
        when(experimentImportRepository.save(any(ExperimentImport.class))).thenReturn(experimentImport);
        when(experimentImport.getErrorMessages()).thenReturn(Collections.singletonList("Validation error"));

        ImportDto result = experimentImportService.preprocess(multipartFile, securedInfo);

        assertNotNull(result);
        assertEquals(ExperimentImportStatus.ERROR, result.getStatus());
        verify(experimentImportAsyncService, never()).process(any(ExperimentImport.class), eq(securedInfo));
    }*/

    @Test
    void testAcknowledgeCompleteDeleted() {
        when(experimentImport.isDeleted()).thenReturn(true);

        experimentImportService.acknowledge(experimentImport, ExperimentImportStatus.COMPLETE_ACKNOWLEDGED);

        verify(experimentImportRepository, never()).save(experimentImport);
    }

    @Test
    void testAcknowledgeErrorDeleted() {
        when(experimentImport.isDeleted()).thenReturn(true);

        experimentImportService.acknowledge(experimentImport, ExperimentImportStatus.ERROR_ACKNOWLEDGED);

        verify(experimentImportRepository, never()).save(experimentImport);
    }

    @Test
    void testValidateExportNotFound() {
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(null);

        experimentImportService.validate(experimentImport);

        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage("No import .zip file found.");
    }

    /*@Test
    void testValidateJsonFileNotFound() {
        File mockDirectory = mock(File.class);
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(mockDirectory);
        when(FileUtils.getFile(mockDirectory, ExperimentImport.JSON_FILE_NAME)).thenReturn(file);

        experimentImportService.validate(experimentImport);

        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("No JSON file [%s] found in imported .zip file.", ExperimentImport.JSON_FILE_NAME));
    }*/

    @Test
    void testToDto() {
        UUID uuid = UUID.randomUUID();
        when(experimentImport.getUuid()).thenReturn(uuid);
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.PROCESSING);
        when(experimentImport.getErrors()).thenReturn(Collections.emptyList());

        ImportDto result = experimentImportService.toDto(experimentImport);

        assertNotNull(result);
        assertEquals(uuid, result.getId());
        assertEquals(ExperimentImportStatus.PROCESSING, result.getStatus());
        assertTrue(CollectionUtils.isEmpty(result.getErrorMessages()));
    }

    @Test
    void testAcknowledgeComplete() {
        when(experimentImport.isDeleted()).thenReturn(false);

        experimentImportService.acknowledge(experimentImport, ExperimentImportStatus.COMPLETE_ACKNOWLEDGED);

        verify(experimentImportRepository).save(experimentImport);
        verify(experimentImport).setStatus(ExperimentImportStatus.COMPLETE_ACKNOWLEDGED);
    }

    @Test
    void testAcknowledgeError() {
        when(experimentImport.isDeleted()).thenReturn(false);

        experimentImportService.acknowledge(experimentImport, ExperimentImportStatus.ERROR_ACKNOWLEDGED);

        verify(experimentImportRepository).save(experimentImport);
        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR_ACKNOWLEDGED);
    }

}
