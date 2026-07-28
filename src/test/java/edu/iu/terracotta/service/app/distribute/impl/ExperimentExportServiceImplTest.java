package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.exceptions.ExperimentExportException;

class ExperimentExportServiceImplTest extends BaseTest {

    @InjectMocks private ExperimentExportServiceImpl experimentExportService;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        setup();

        when(experiment.getTitle()).thenReturn("My Experiment");
        when(group.getExperiment()).thenReturn(experiment);
        when(platformDeployment.getBaseUrl()).thenReturn("https://institution.example.edu");
        when(ltiContextEntity.getTitle()).thenReturn("Course Title");
        when(lmsUtils.sanitize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByAssessment_Treatment_Condition_Experiment_ExperimentId(anyLong())).thenReturn(List.of(question));
        when(answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(anyLong())).thenReturn(List.of(answerMc));
    }

    private Export captureExport() throws ExperimentExportException, IOException {
        ArgumentCaptor<Export> captor = ArgumentCaptor.forClass(Export.class);
        experimentExportService.export(experiment);
        verify(fileStorageService).createExperimentExportFile(any(ExportDto.class), captor.capture(), anyString());

        return captor.getValue();
    }

    @Test
    void testExportSuccess() throws ExperimentExportException, IOException {
        ExportDto result = experimentExportService.export(experiment);

        assertNotNull(result);
        assertTrue(result.getFilename().endsWith(".zip"));
        assertEquals("application/zip", result.getMimeType());
        verify(fileStorageService).createExperimentExportFile(any(ExportDto.class), any(Export.class), anyString());
    }

    @Test
    void testExportMapsExperimentAndCollections() throws ExperimentExportException, IOException {
        Export export = captureExport();

        assertEquals(1, export.getAnswersMc().size());
        assertEquals(1, export.getAssessments().size());
        assertEquals(1, export.getAssignments().size());
        assertEquals(1, export.getConditions().size());
        assertEquals(1, export.getExposureGroupConditions().size());
        assertEquals(1, export.getExposures().size());
        assertEquals(1, export.getGroups().size());
        assertEquals(1, export.getOutcomes().size());
        assertEquals(1, export.getQuestions().size());
        assertEquals(1, export.getTreatments().size());
        assertEquals(1L, export.getExperiment().getId());
        assertEquals(ParticipationTypes.AUTO, export.getExperiment().getParticipationType());
        assertNull(export.getConsentDocument());
        assertEquals("https://institution.example.edu", export.getOrigin().getInstitutionUrl());
        assertEquals("Course Title", export.getOrigin().getCourseTitle());
    }

    @Test
    void testExportWithConsentDocument() throws ExperimentExportException, IOException {
        when(experiment.getParticipationType()).thenReturn(ParticipationTypes.CONSENT);
        when(consentDocument.getHtml()).thenReturn("<p>consent</p>");
        when(consentDocument.getTitle()).thenReturn("Consent Title");
        when(consentDocument.getConsentDocumentId()).thenReturn(9L);

        Export export = captureExport();

        assertNotNull(export.getConsentDocument());
        assertEquals("<p>consent</p>", export.getConsentDocument().getHtml());
        assertEquals("Consent Title", export.getConsentDocument().getTitle());
        assertEquals(9L, export.getConsentDocument().getId());
        assertEquals(1L, export.getConsentDocument().getExperimentId());
    }

    @Test
    void testExportWithIntegrationQuestion() throws ExperimentExportException, IOException {
        when(question.isIntegration()).thenReturn(true);
        when(integrationClient.isEnabled()).thenReturn(true);
        when(integration.getId()).thenReturn(5L);

        Export export = captureExport();

        assertEquals(1, export.getIntegrations().size());
        assertEquals(1, export.getIntegrationClients().size());
        assertEquals(1, export.getIntegrationConfigurations().size());
        assertTrue(export.getIntegrationClients().get(0).isEnabled());
        assertEquals(5L, export.getIntegrations().get(0).getId());
        assertEquals(1L, export.getIntegrations().get(0).getQuestionId());
    }

    @Test
    void testExportWithMcQuestionRandomizeAnswers() throws ExperimentExportException, IOException {
        when(question.isMC()).thenReturn(true);
        when(questionMcRepository.findByQuestionId(anyLong())).thenReturn(Optional.of(questionMc));
        when(questionMc.isRandomizeAnswers()).thenReturn(true);

        Export export = captureExport();

        assertTrue(export.getQuestions().get(0).isRandomizeAnswers());
    }

    @Test
    void testExportDedupesAssignmentsByTreatment() throws ExperimentExportException, IOException {
        Treatment secondTreatment = mock(Treatment.class);
        when(secondTreatment.getAssessment()).thenReturn(assessment);
        when(secondTreatment.getAssignment()).thenReturn(assignment);
        when(secondTreatment.getCondition()).thenReturn(condition);
        when(secondTreatment.getTreatmentId()).thenReturn(2L);
        when(treatmentRepository.findByCondition_Experiment_ExperimentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(List.of(treatment, secondTreatment));

        Export export = captureExport();

        assertEquals(1, export.getAssignments().size());
        assertEquals(2, export.getTreatments().size());
    }

    @Test
    void testExportThrowsExperimentExportExceptionOnIoException() throws IOException {
        doThrow(new IOException("disk full")).when(fileStorageService).createExperimentExportFile(any(ExportDto.class), any(Export.class), anyString());

        ExperimentExportException exception = assertThrows(ExperimentExportException.class, () -> experimentExportService.export(experiment));

        assertEquals("Error occurred creating experiment ID: [1] export", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void testDistinctByKeyFiltersDuplicates() {
        Predicate<String> predicate = ExperimentExportServiceImpl.distinctByKey(String::length);

        assertTrue(predicate.test("a"));
        assertFalse(predicate.test("b"));
        assertTrue(predicate.test("bb"));
    }

}
