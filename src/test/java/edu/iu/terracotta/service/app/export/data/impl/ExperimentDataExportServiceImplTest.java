package edu.iu.terracotta.service.app.export.data.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.model.dto.export.data.ExperimentDataExportDto;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.dao.repository.messaging.log.MessageLogRepository;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportException;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportNotFoundException;
import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.service.app.async.ExperimentDataExportAsyncService;

class ExperimentDataExportServiceImplTest extends BaseTest {

    private static final long TTL_SECONDS = 600L;

    @Mock private ExperimentDataExportRepository experimentDataExportRepository;
    @Mock private MessageLogRepository messageLogRepository;
    @Mock private ExperimentDataExportAsyncService experimentDataExportAsyncService;

    @InjectMocks private ExperimentDataExportServiceImpl experimentDataExportService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();
        ReflectionTestUtils.setField(experimentDataExportService, "processingTtlSeconds", TTL_SECONDS);

        when(experiment.getTitle()).thenReturn("My Experiment");
    }

    private ExperimentDataExport buildExportData(ExperimentDataExportStatus status, Timestamp createdAt, Timestamp updatedAt) {
        ExperimentDataExport exportData = ExperimentDataExport.builder()
            .experiment(experiment)
            .owner(ltiUserEntity)
            .status(status)
            .build();
        exportData.setId(10L);
        exportData.setUuid(UUID.randomUUID());
        exportData.setCreatedAt(createdAt);
        exportData.setUpdatedAt(updatedAt);

        return exportData;
    }

    @Test
    void testProcessSuccess() throws Exception {
        when(experimentDataExportRepository.save(any(ExperimentDataExport.class))).thenAnswer(invocation -> {
            ExperimentDataExport arg = invocation.getArgument(0);
            arg.setId(55L);
            arg.setUuid(UUID.randomUUID());

            return arg;
        });

        ExperimentDataExportDto result = experimentDataExportService.process(experiment, securedInfo);

        assertEquals(ExperimentDataExportStatus.PROCESSING, result.getStatus());
        assertEquals(1L, result.getExperimentId());
        assertEquals("My Experiment", result.getExperimentTitle());
        assertNull(result.getFile());
        verify(experimentDataExportAsyncService).process(eq(55L), eq(securedInfo));
    }

    @Test
    void testPollNotFound() {
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());

        assertThrows(
            ExperimentDataExportNotFoundException.class,
            () -> experimentDataExportService.poll(experiment, securedInfo, false)
        );
    }

    @Test
    void testPollProcessingTtlExceededSetsError() {
        Timestamp updatedAt = Timestamp.from(Instant.now().minusSeconds(TTL_SECONDS + 100));
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.PROCESSING, updatedAt, updatedAt);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(exportData));

        ExperimentDataExportException exception = assertThrows(
            ExperimentDataExportException.class,
            () -> experimentDataExportService.poll(experiment, securedInfo, false)
        );

        assertTrue(exception.getMessage().contains("has been in a processing state"));
        assertEquals(ExperimentDataExportStatus.ERROR, exportData.getStatus());
        verify(experimentDataExportRepository).save(exportData);
    }

    @Test
    void testPollOutdatedAcknowledgedThrowsNotFound() {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.OUTDATED_ACKNOWLEDGED, now, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(exportData));

        assertThrows(
            ExperimentDataExportNotFoundException.class,
            () -> experimentDataExportService.poll(experiment, securedInfo, false)
        );
    }

    @Test
    void testPollCurrentReturnsExistingWithoutSave() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(exportData));
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.empty());
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());

        ExperimentDataExportDto result = experimentDataExportService.poll(experiment, securedInfo, false);

        assertEquals(ExperimentDataExportStatus.READY, result.getStatus());
        verify(experimentDataExportRepository, never()).save(any(ExperimentDataExport.class));
    }

    @Test
    void testPollOutdatedNoNewCreatesReturnsOutdated() throws Exception {
        Timestamp createdAt = Timestamp.from(Instant.now().minusSeconds(60));
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, createdAt, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(exportData));
        when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now().plusSeconds(60)));
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.of(submission));
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());

        ExperimentDataExportDto result = experimentDataExportService.poll(experiment, securedInfo, false);

        assertEquals(ExperimentDataExportStatus.OUTDATED, result.getStatus());
        verify(experimentDataExportRepository).save(exportData);
        verify(experimentDataExportAsyncService, never()).process(anyLong(), any());
    }

    @Test
    void testPollOutdatedCreateNewReprocesses() throws Exception {
        Timestamp createdAt = Timestamp.from(Instant.now().minusSeconds(60));
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, createdAt, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.of(exportData));
        when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now().plusSeconds(60)));
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.of(submission));
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(experimentDataExportRepository.save(any(ExperimentDataExport.class))).thenAnswer(invocation -> {
            ExperimentDataExport arg = invocation.getArgument(0);

            if (arg.getId() == null) {
                arg.setId(77L);
                arg.setUuid(UUID.randomUUID());
            }

            return arg;
        });

        ExperimentDataExportDto result = experimentDataExportService.poll(experiment, securedInfo, true);

        assertEquals(ExperimentDataExportStatus.REPROCESSING, result.getStatus());
        verify(experimentDataExportRepository, times(2)).save(any(ExperimentDataExport.class));
        verify(experimentDataExportAsyncService).process(eq(77L), eq(securedInfo));
    }

    @Test
    void testPollListFiltersNotFound() throws Exception {
        Experiment experiment2 = mock(Experiment.class);
        when(experiment2.getExperimentId()).thenReturn(2L);
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(exportData));
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(2L)).thenReturn(Optional.empty());
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.empty());
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());

        List<ExperimentDataExportDto> result = experimentDataExportService.poll(List.of(experiment, experiment2), securedInfo, false);

        assertEquals(1, result.size());
        assertEquals(ExperimentDataExportStatus.READY, result.get(0).getStatus());
    }

    @Test
    void testPollListSwallowsOtherExceptions() throws Exception {
        Experiment experiment2 = mock(Experiment.class);
        when(experiment2.getExperimentId()).thenReturn(2L);
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(exportData));
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(2L)).thenThrow(new NumberFormatException("bad number"));
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.empty());
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());

        List<ExperimentDataExportDto> result = experimentDataExportService.poll(List.of(experiment, experiment2), securedInfo, false);

        assertEquals(1, result.size());
    }

    @Test
    void testRetrieveExistingAvailable() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.of(exportData));
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.empty());
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(fileStorageService.getExperimentDataExport(10L)).thenReturn(file);

        ExperimentDataExportDto result = experimentDataExportService.retrieve(UUID.randomUUID(), experiment, securedInfo);

        assertEquals(ExperimentDataExportStatus.DOWNLOADED, exportData.getStatus());
        assertEquals(file, result.getFile());
        verify(experimentDataExportRepository).save(exportData);
    }

    @Test
    void testRetrieveNoneAvailableProcessesNew() throws Exception {
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.empty());
        when(experimentDataExportRepository.save(any(ExperimentDataExport.class))).thenAnswer(invocation -> {
            ExperimentDataExport arg = invocation.getArgument(0);
            arg.setId(88L);
            arg.setUuid(UUID.randomUUID());

            return arg;
        });

        ExperimentDataExportDto result = experimentDataExportService.retrieve(UUID.randomUUID(), experiment, securedInfo);

        assertEquals(ExperimentDataExportStatus.PROCESSING, result.getStatus());
        verify(experimentDataExportAsyncService).process(eq(88L), eq(securedInfo));
    }

    @Test
    void testFindLatestAvailableEmptyWhenNoneFound() throws Exception {
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.empty());

        Optional<ExperimentDataExport> result = experimentDataExportService.findLatestAvailableExperimentDataExport(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindLatestAvailableEmptyWhenOutdated() throws Exception {
        Timestamp createdAt = Timestamp.from(Instant.now().minusSeconds(60));
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, createdAt, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.of(exportData));
        when(submission.getDateSubmitted()).thenReturn(Timestamp.from(Instant.now().plusSeconds(60)));
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.of(submission));
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());

        Optional<ExperimentDataExport> result = experimentDataExportService.findLatestAvailableExperimentDataExport(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindLatestAvailablePresentWhenCurrent() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        when(experimentDataExportRepository.findTopByExperiment_ExperimentIdAndStatusInOrderByCreatedAtDesc(anyLong(), any())).thenReturn(Optional.of(exportData));
        when(submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(anyLong())).thenReturn(Optional.empty());
        when(messageLogRepository.findTopByMessage_ExposureGroupCondition_Condition_Experiment_ExperimentIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(outcomeRepository.findTopByExposure_Experiment_ExperimentIdOrderByUpdatedAtDesc(anyLong())).thenReturn(Optional.empty());

        Optional<ExperimentDataExport> result = experimentDataExportService.findLatestAvailableExperimentDataExport(1L);

        assertEquals(exportData, result.get());
    }

    @Test
    void testAcknowledgeSuccess() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        UUID uuid = UUID.randomUUID();
        when(experimentDataExportRepository.findByUuidAndExperiment_ExperimentId(uuid, 1L)).thenReturn(Optional.of(exportData));
        when(experimentDataExportRepository.save(exportData)).thenReturn(exportData);

        ExperimentDataExportDto result = experimentDataExportService.acknowledge(uuid, experiment, ExperimentDataExportStatus.READY_ACKNOWLEDGED);

        assertEquals(ExperimentDataExportStatus.READY_ACKNOWLEDGED, exportData.getStatus());
        assertEquals(ExperimentDataExportStatus.READY_ACKNOWLEDGED, result.getStatus());
    }

    @Test
    void testAcknowledgeNotFound() {
        when(experimentDataExportRepository.findByUuidAndExperiment_ExperimentId(any(UUID.class), anyLong())).thenReturn(Optional.empty());

        assertThrows(
            ExperimentDataExportNotFoundException.class,
            () -> experimentDataExportService.acknowledge(UUID.randomUUID(), experiment, ExperimentDataExportStatus.READY_ACKNOWLEDGED)
        );
    }

    @Test
    void testToDtoIncludesFileContentWhenRequested() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        exportData.setFileName("export.zip");
        when(fileStorageService.getExperimentDataExport(10L)).thenReturn(file);

        ExperimentDataExportDto result = experimentDataExportService.toDto(exportData, true);

        assertEquals(file, result.getFile());
        assertEquals("export.zip", result.getFileName());
    }

    @Test
    void testToDtoExcludesFileContentWhenNotRequested() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);

        ExperimentDataExportDto result = experimentDataExportService.toDto(exportData, false);

        assertNull(result.getFile());
        verify(fileStorageService, never()).getExperimentDataExport(anyLong());
    }

    @Test
    void testToDtoBlankFileNameReturnsNull() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        ExperimentDataExport exportData = buildExportData(ExperimentDataExportStatus.READY, now, now);
        exportData.setFileName("");

        ExperimentDataExportDto result = experimentDataExportService.toDto(exportData, false);

        assertNull(result.getFileName());
    }

}
