package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImportError;
import edu.iu.terracotta.dao.model.distribute.export.AnswerMcExport;
import edu.iu.terracotta.dao.model.distribute.export.AssessmentExport;
import edu.iu.terracotta.dao.model.distribute.export.AssignmentExport;
import edu.iu.terracotta.dao.model.distribute.export.ConditionExport;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.distribute.export.ExperimentExport;
import edu.iu.terracotta.dao.model.distribute.export.ExposureExport;
import edu.iu.terracotta.dao.model.distribute.export.ExposureGroupConditionExport;
import edu.iu.terracotta.dao.model.distribute.export.GroupExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationClientExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationConfigurationExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationExport;
import edu.iu.terracotta.dao.model.distribute.export.OutcomeExport;
import edu.iu.terracotta.dao.model.distribute.export.QuestionExport;
import edu.iu.terracotta.dao.model.distribute.export.TreatmentExport;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import tools.jackson.databind.json.JsonMapper;

class ExperimentImportServiceImplTest extends BaseTest {

    @InjectMocks private ExperimentImportServiceImpl experimentImportService;

    private Path importDirectory;

    @BeforeEach
    void beforeEach() throws IOException {
        MockitoAnnotations.openMocks(this);
        setup();

        importDirectory = Files.createTempDirectory("experiment-import-test");
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(importDirectory.toFile());
    }

    /**
     * Builds a fully valid, fully cross-referenced Export graph: one of every
     * component, each one correctly referencing the id of the component it
     * depends on. Individual tests mutate a single field via the generated
     * setters to trigger exactly one validation failure.
     */
    private Export fullExport() {
        return Export.builder()
            .experiment(
                ExperimentExport.builder()
                    .id(1L)
                    .title("source experiment title")
                    .participationType(ParticipationTypes.AUTO)
                    .build()
            )
            .conditions(List.of(ConditionExport.builder().id(10L).name("condition").experimentId(1L).build()))
            .exposures(List.of(ExposureExport.builder().id(20L).title("exposure").experimentId(1L).build()))
            .groups(List.of(GroupExport.builder().id(30L).name("group").experimentId(1L).build()))
            .exposureGroupConditions(List.of(ExposureGroupConditionExport.builder().id(40L).exposureId(20L).groupId(30L).conditionId(10L).build()))
            .assignments(List.of(AssignmentExport.builder().id(50L).title("assignment").exposureId(20L).build()))
            .treatments(List.of(TreatmentExport.builder().id(60L).conditionId(10L).assignmentId(50L).build()))
            .assessments(List.of(AssessmentExport.builder().id(70L).title("assessment").treatmentId(60L).build()))
            .questions(List.of(QuestionExport.builder().id(80L).html("question").questionType(QuestionTypes.MC).assessmentId(70L).questionOrder(1).build()))
            .integrationClients(List.of(IntegrationClientExport.builder().id(90L).name("integration client").enabled(true).build()))
            .integrationConfigurations(List.of(IntegrationConfigurationExport.builder().id(91L).clientId(90L).launchUrl("http://launch.url").build()))
            .integrations(List.of(IntegrationExport.builder().id(92L).configurationId(91L).questionId(80L).build()))
            .answersMc(List.of(AnswerMcExport.builder().id(93L).answerOrder(1).correct(true).html("answer").questionId(80L).build()))
            .outcomes(List.of(OutcomeExport.builder().id(94L).title("outcome").maxPoints(10F).exposureId(20L).build()))
            .build();
    }

    private void writeExportJson(Export export) throws IOException {
        JsonMapper.builder().build().writeValue(importDirectory.resolve(ExperimentImport.JSON_FILE_NAME).toFile(), export);
    }

    private void assertValidationError(Export export, String expectedMessage) throws IOException {
        writeExportJson(export);

        experimentImportService.validate(experimentImport);

        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(expectedMessage);
    }

    @Test
    void testPreprocessSuccess() throws IOException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.zip");
        when(experimentImport.getErrors()).thenReturn(Collections.emptyList());

        Path jsonFile = importDirectory.resolve(ExperimentImport.JSON_FILE_NAME);
        JsonMapper.builder().build().writeValue(jsonFile.toFile(), fullExport());

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getFile(any(File.class), anyString())).thenReturn(jsonFile.toFile());

            ImportDto result = experimentImportService.preprocess(multipartFile, securedInfo);

            assertNotNull(result);
        }

        verify(fileStorageService).saveExperimentImportFile(eq(multipartFile), any(ExperimentImport.class));
        verify(experimentImportAsyncService).process(any(ExperimentImport.class), eq(securedInfo));
    }

    @Test
    void testPreprocessContextNotFound() {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(ltiContextRepository.findById(1L)).thenReturn(Optional.empty());

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> {
            experimentImportService.preprocess(multipartFile, securedInfo);
        });

        assertEquals("Context ID: [1] not found", exception.getMessage());
    }

    @Test
    void testPreprocessValidationError() throws IOException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.zip");
        when(experimentImport.getErrors()).thenReturn(List.of(mock(ExperimentImportError.class)));
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.ERROR);

        Export export = fullExport();
        export.getExperiment().setTitle(" ");
        Path jsonFile = importDirectory.resolve(ExperimentImport.JSON_FILE_NAME);
        JsonMapper.builder().build().writeValue(jsonFile.toFile(), export);

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getFile(any(File.class), anyString())).thenReturn(jsonFile.toFile());

            ImportDto result = experimentImportService.preprocess(multipartFile, securedInfo);

            assertNotNull(result);
            assertEquals(ExperimentImportStatus.ERROR, result.getStatus());
        }

        verify(experimentImportAsyncService, never()).process(any(ExperimentImport.class), eq(securedInfo));
        verify(experimentImportErrorRepository).save(any(ExperimentImportError.class));
    }

    @Test
    void testPreprocessError() {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.zip");
        when(experimentImport.getUuid()).thenReturn(UUID.randomUUID());
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.ERROR);
        when(experimentImport.getErrors()).thenReturn(Collections.emptyList());

        ImportDto result = experimentImportService.preprocessError(multipartFile, "boom", securedInfo);

        assertNotNull(result);
        assertEquals(ExperimentImportStatus.ERROR, result.getStatus());
        verify(experimentImportRepository).save(any(ExperimentImport.class));
    }

    @Test
    void testPreprocessErrorContextNotFound() {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(ltiContextRepository.findById(1L)).thenReturn(Optional.empty());

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> {
            experimentImportService.preprocessError(multipartFile, "boom", securedInfo);
        });

        assertEquals("Context ID: [1] not found", exception.getMessage());
    }

    @Test
    void testGetAll() {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentImportRepository.findAllByOwner_UserKeyAndContext_ContextIdAndStatusIn(eq("user-id"), eq(1L), anyList())).thenReturn(List.of(experimentImport));
        when(experimentImport.getUuid()).thenReturn(UUID.randomUUID());
        when(experimentImport.getStatus()).thenReturn(ExperimentImportStatus.PROCESSING);
        when(experimentImport.getErrors()).thenReturn(Collections.emptyList());

        List<ImportDto> result = experimentImportService.getAll(securedInfo);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

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
        assertTrue(CollectionUtils.isNotEmpty(result.getErrorMessages()));
    }

    @Test
    void testValidateExportNotFound() {
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(null);

        experimentImportService.validate(experimentImport);

        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage("No import .zip file found.");
    }

    @Test
    void testValidateJsonFileNotFound() {
        File mockDirectory = mock(File.class);
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(mockDirectory);

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getFile(mockDirectory, ExperimentImport.JSON_FILE_NAME)).thenReturn(file);

            experimentImportService.validate(experimentImport);
        }

        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("No JSON file [%s] found in imported .zip file.", ExperimentImport.JSON_FILE_NAME));
    }

    @Test
    void testValidateJsonParseError() throws IOException {
        Files.writeString(importDirectory.resolve(ExperimentImport.JSON_FILE_NAME), "not valid json");

        experimentImportService.validate(experimentImport);

        verify(experimentImport).setStatus(ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("Error reading JSON file: [%s]", ExperimentImport.JSON_FILE_NAME));
    }

    @Test
    void testValidateSuccessNoErrors() throws IOException {
        writeExportJson(fullExport());

        experimentImportService.validate(experimentImport);

        verify(experimentImport).setSourceTitle("source experiment title");
        verify(experimentImportRepository).save(experimentImport);
        verify(experimentImport, never()).setStatus(ExperimentImportStatus.ERROR);
        verify(experimentImport, never()).addErrorMessage(anyString());
    }

    @Test
    void testValidateConsentDocumentSuccess() throws IOException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        writeExportJson(export);

        File consentDir = importDirectory.resolve("consent").toFile();
        consentDir.mkdirs();
        Files.writeString(consentDir.toPath().resolve(ExperimentImport.CONSENT_FILE_NAME), "pdf-bytes");

        experimentImportService.validate(experimentImport);

        verify(experimentImport, never()).setStatus(ExperimentImportStatus.ERROR);
    }

    @Test
    void testValidateConsentDocumentMissing() throws IOException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);

        assertValidationError(export, String.format("No consent PDF file [%s] found for experiment with consent participation type.", ExperimentImport.CONSENT_FILE_NAME));
    }

    @Test
    void testValidateExperimentTitleBlank() throws IOException {
        Export export = fullExport();
        export.getExperiment().setTitle(" ");

        assertValidationError(export, "Experiment title cannot be blank.");
    }

    @Test
    void testValidateConditionExperimentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getConditions().get(0).setExperimentId(999L);

        assertValidationError(export, "No experiment ID: [999] found for condition ID: [10]");
    }

    @Test
    void testValidateConditionNameBlank() throws IOException {
        Export export = fullExport();
        export.getConditions().get(0).setName("");

        assertValidationError(export, "Condition with ID: [10] :: name cannot be blank.");
    }

    @Test
    void testValidateExposureExperimentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getExposures().get(0).setExperimentId(999L);

        assertValidationError(export, "No experiment ID: [999] found for exposure ID: [20]");
    }

    @Test
    void testValidateExposureTitleBlank() throws IOException {
        Export export = fullExport();
        export.getExposures().get(0).setTitle("");

        assertValidationError(export, "Exposure with ID: [20] :: title cannot be blank.");
    }

    @Test
    void testValidateGroupExperimentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getGroups().get(0).setExperimentId(999L);

        assertValidationError(export, "No experiment ID: [999] found for group ID: [30]");
    }

    @Test
    void testValidateGroupNameBlank() throws IOException {
        Export export = fullExport();
        export.getGroups().get(0).setName("");

        assertValidationError(export, "Group with ID: [30] :: name cannot be blank.");
    }

    @Test
    void testValidateExposureGroupConditionExposureIdMismatch() throws IOException {
        Export export = fullExport();
        export.getExposureGroupConditions().get(0).setExposureId(999L);

        assertValidationError(export, "No exposure ID: [999] found for exposureGroupCondition ID: [40]");
    }

    @Test
    void testValidateExposureGroupConditionGroupIdMismatch() throws IOException {
        Export export = fullExport();
        export.getExposureGroupConditions().get(0).setGroupId(999L);

        assertValidationError(export, "No group ID: [999] found for exposureGroupCondition ID: [40]");
    }

    @Test
    void testValidateExposureGroupConditionConditionIdMismatch() throws IOException {
        Export export = fullExport();
        export.getExposureGroupConditions().get(0).setConditionId(999L);

        assertValidationError(export, "No condition ID: [999] found for exposureGroupCondition ID: [40]");
    }

    @Test
    void testValidateAssignmentExposureIdMismatch() throws IOException {
        Export export = fullExport();
        export.getAssignments().get(0).setExposureId(999L);

        assertValidationError(export, "No exposure ID: [999] found for assignment ID: [50]");
    }

    @Test
    void testValidateAssignmentTitleBlank() throws IOException {
        Export export = fullExport();
        export.getAssignments().get(0).setTitle("");

        assertValidationError(export, "Assignment with ID: [50] :: title cannot be blank.");
    }

    @Test
    void testValidateTreatmentConditionIdMismatch() throws IOException {
        Export export = fullExport();
        export.getTreatments().get(0).setConditionId(999L);

        assertValidationError(export, "No condition ID: [999] found for treatment ID: [60]");
    }

    @Test
    void testValidateTreatmentAssignmentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getTreatments().get(0).setAssignmentId(999L);

        assertValidationError(export, "No assignment ID: [999] found for treatment ID: [60]");
    }

    @Test
    void testValidateAssessmentTreatmentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getAssessments().get(0).setTreatmentId(999L);

        assertValidationError(export, "No treatment ID: [999] found for assessment ID: [70]");
    }

    @Test
    void testValidateQuestionAssessmentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getQuestions().get(0).setAssessmentId(999L);

        assertValidationError(export, "No assessment ID: [999] found for question ID: [80]");
    }

    @Test
    void testValidateQuestionIntegrationIdMismatch() throws IOException {
        Export export = fullExport();
        export.getQuestions().get(0).setIntegrationId(999L);

        assertValidationError(export, "No integration ID: [999] found for question ID: [80]");
    }

    @Test
    void testValidateQuestionOrderNull() throws IOException {
        Export export = fullExport();
        export.getQuestions().get(0).setQuestionOrder(null);

        assertValidationError(export, "Question with ID: [80] :: order cannot be null.");
    }

    @Test
    void testValidateIntegrationConfigurationClientIdMismatch() throws IOException {
        Export export = fullExport();
        export.getIntegrationConfigurations().get(0).setClientId(999L);

        assertValidationError(export, "No integration client ID: [999] found for integration configuration ID: [91]");
    }

    @Test
    void testValidateIntegrationConfigurationIdMismatch() throws IOException {
        Export export = fullExport();
        export.getIntegrations().get(0).setConfigurationId(999L);

        assertValidationError(export, "No integration configuration ID: [999] found for integration ID: [92]");
    }

    @Test
    void testValidateIntegrationQuestionIdMismatch() throws IOException {
        Export export = fullExport();
        export.getIntegrations().get(0).setQuestionId(999L);

        assertValidationError(export, "No question ID: [999] found for integration ID: [92]");
    }

    @Test
    void testValidateAnswerMcQuestionIdMismatch() throws IOException {
        Export export = fullExport();
        export.getAnswersMc().get(0).setQuestionId(999L);

        assertValidationError(export, "No question ID: [999] found for multiple choice answer ID: [93]");
    }

    @Test
    void testValidateAnswerMcOrderNull() throws IOException {
        Export export = fullExport();
        export.getAnswersMc().get(0).setAnswerOrder(null);

        assertValidationError(export, "AnswerMc with ID: [93] :: order cannot be null.");
    }

    @Test
    void testValidateOutcomeExposureIdMismatch() throws IOException {
        Export export = fullExport();
        export.getOutcomes().get(0).setExposureId(999L);

        assertValidationError(export, "No exposure ID: [999] found for outcome ID: [94]");
    }

}
