package edu.iu.terracotta.service.app.distribute.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
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

        // entityManager isn't a constructor-injected (final) field, and Mockito's field-injection
        // fallback for @InjectMocks doesn't reliably reach it here - wire it explicitly
        ReflectionTestUtils.setField(experimentImportService, "entityManager", entityManager);

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
                    .id("1")
                    .title("source experiment title")
                    .participationType(ParticipationTypes.AUTO)
                    .build()
            )
            .conditions(List.of(ConditionExport.builder().id("10").name("condition").experimentId("1").build()))
            .exposures(List.of(ExposureExport.builder().id("20").title("exposure").experimentId("1").build()))
            .groups(List.of(GroupExport.builder().id("30").name("group").experimentId("1").build()))
            .exposureGroupConditions(List.of(ExposureGroupConditionExport.builder().id("40").exposureId("20").groupId("30").conditionId("10").build()))
            .assignments(List.of(AssignmentExport.builder().id("50").title("assignment").exposureId("20").build()))
            .treatments(List.of(TreatmentExport.builder().id("60").conditionId("10").assignmentId("50").build()))
            .assessments(List.of(AssessmentExport.builder().id("70").title("assessment").treatmentId("60").build()))
            .questions(List.of(QuestionExport.builder().id("80").html("question").questionType(QuestionTypes.MC).assessmentId("70").questionOrder(1).build()))
            .integrationClients(List.of(IntegrationClientExport.builder().id("90").name("integration client").enabled(true).build()))
            .integrationConfigurations(List.of(IntegrationConfigurationExport.builder().id("91").clientId("90").launchUrl("http://launch.url").build()))
            .integrations(List.of(IntegrationExport.builder().id("92").configurationId("91").questionId("80").build()))
            .answersMc(List.of(AnswerMcExport.builder().id("93").answerOrder(1).correct(true).html("answer").questionId("80").build()))
            .outcomes(List.of(OutcomeExport.builder().id("94").title("outcome").maxPoints(10F).exposureId("20").build()))
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
        verify(experimentImportAsyncService).process(any(ExperimentImport.class), eq(securedInfo), eq(Map.of()), eq(false), eq(false));
    }

    // used by ExperimentCopyCandidateServiceImpl to feed an in-process export straight into this
    // same import pipeline, without a real uploaded MultipartFile
    @Test
    void testPreprocessFromFileSuccess() throws IOException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentImport.getErrors()).thenReturn(Collections.emptyList());

        Path jsonFile = importDirectory.resolve(ExperimentImport.JSON_FILE_NAME);
        JsonMapper.builder().build().writeValue(jsonFile.toFile(), fullExport());

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getFile(any(File.class), anyString())).thenReturn(jsonFile.toFile());

            ImportDto result = experimentImportService.preprocessFromFile(file, "test-file.zip", securedInfo, Map.of(), true);

            assertNotNull(result);
        }

        verify(fileStorageService).saveExperimentImportFile(eq(file), any(ExperimentImport.class));
        verify(experimentImportAsyncService).process(any(ExperimentImport.class), eq(securedInfo), eq(Map.of()), eq(true), eq(true));
    }

    // validate(...) saves the entity again partway through (to persist the source title),
    // returning a different (more current) instance than the one passed in - regression test for
    // a bug where that returned reference was discarded, letting the async process(...) call
    // receive an already-superseded entity and fail to save its own final status update with an
    // optimistic-locking error (every save() call returns a fresh instance in real Hibernate
    // usage, unlike this test's other cases where a single shared mock stands in for all of them)
    @Test
    void testPreprocessFromFilePassesPostValidationEntityToAsyncProcess() throws IOException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);

        ExperimentImport preValidation = mock(ExperimentImport.class);
        ExperimentImport postValidation = mock(ExperimentImport.class);
        when(postValidation.getErrors()).thenReturn(Collections.emptyList());

        when(experimentImportRepository.save(any(ExperimentImport.class)))
            .thenReturn(preValidation)
            .thenReturn(postValidation);

        Path jsonFile = importDirectory.resolve(ExperimentImport.JSON_FILE_NAME);
        JsonMapper.builder().build().writeValue(jsonFile.toFile(), fullExport());

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getFile(any(File.class), anyString())).thenReturn(jsonFile.toFile());

            experimentImportService.preprocessFromFile(file, "test-file.zip", securedInfo, Map.of(), true);
        }

        verify(experimentImportAsyncService).process(eq(postValidation), eq(securedInfo), eq(Map.of()), eq(true), eq(true));
        verify(experimentImportAsyncService, never()).process(eq(preValidation), any(), anyMap(), anyBoolean(), anyBoolean());
    }

    // the map is forwarded unchanged, all the way through to the async import step - this is
    // what lets ExperimentCopyCandidateServiceImpl's repoint-instead-of-duplicate logic reach the
    // assignment-creation step despite it running on a different (@Async) thread
    @Test
    void testPreprocessFromFileForwardsNonEmptyRepointMap() throws IOException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(experimentImport.getErrors()).thenReturn(Collections.emptyList());

        Path jsonFile = importDirectory.resolve(ExperimentImport.JSON_FILE_NAME);
        JsonMapper.builder().build().writeValue(jsonFile.toFile(), fullExport());

        LmsAssignment existingLmsAssignment = mock(LmsAssignment.class);
        Map<Long, LmsAssignment> assignmentRepointMap = Map.of(50L, existingLmsAssignment);

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getFile(any(File.class), anyString())).thenReturn(jsonFile.toFile());

            experimentImportService.preprocessFromFile(file, "test-file.zip", securedInfo, assignmentRepointMap, true);
        }

        verify(experimentImportAsyncService).process(any(ExperimentImport.class), eq(securedInfo), eq(assignmentRepointMap), eq(true), eq(true));
    }

    @Test
    void testPreprocessFromFileContextNotFound() {
        when(securedInfo.getContextId()).thenReturn(1L);
        when(ltiContextRepository.findById(1L)).thenReturn(Optional.empty());

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> {
            experimentImportService.preprocessFromFile(file, "test-file.zip", securedInfo, Map.of(), true);
        });

        assertEquals("Context ID: [1] not found", exception.getMessage());
    }

    // validate(...) saves the entity again partway through (to persist the source title),
    // returning a different (more current) instance than the one passed in - regression test for
    // a bug where that returned reference was discarded, letting the async process(...) call
    // receive an already-superseded entity and fail to save its own final status update with an
    // optimistic-locking error (every save() call returns a fresh instance in real Hibernate
    // usage, unlike this test's other cases where a single shared mock stands in for all of them)
    @Test
    void testPreprocessPassesPostValidationEntityToAsyncProcess() throws IOException {
        when(securedInfo.getUserId()).thenReturn("user-id");
        when(securedInfo.getPlatformDeploymentId()).thenReturn(1L);
        when(securedInfo.getContextId()).thenReturn(1L);
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.zip");

        ExperimentImport preValidation = mock(ExperimentImport.class);
        ExperimentImport postValidation = mock(ExperimentImport.class);
        when(postValidation.getErrors()).thenReturn(Collections.emptyList());

        when(experimentImportRepository.save(any(ExperimentImport.class)))
            .thenReturn(preValidation)
            .thenReturn(postValidation);

        Path jsonFile = importDirectory.resolve(ExperimentImport.JSON_FILE_NAME);
        JsonMapper.builder().build().writeValue(jsonFile.toFile(), fullExport());

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.getFile(any(File.class), anyString())).thenReturn(jsonFile.toFile());

            experimentImportService.preprocess(multipartFile, securedInfo);
        }

        verify(experimentImportAsyncService).process(eq(postValidation), eq(securedInfo), eq(Map.of()), eq(false), eq(false));
        verify(experimentImportAsyncService, never()).process(eq(preValidation), any(), any(), anyBoolean(), anyBoolean());
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

        verify(experimentImportAsyncService, never()).process(any(ExperimentImport.class), eq(securedInfo), anyMap(), anyBoolean(), anyBoolean());
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

    // export id/FK fields are plain strings - fullExport() above uses old-style numeric strings
    // (as a pre-uuid export file would still contain), this proves a new-style export using real
    // uuid strings validates identically, since the cross-referencing is format-agnostic
    @Test
    void testValidateSuccessNoErrorsWithUuidIds() throws IOException {
        String experimentId = UUID.randomUUID().toString();
        String conditionId = UUID.randomUUID().toString();
        String exposureId = UUID.randomUUID().toString();
        String groupId = UUID.randomUUID().toString();
        String exposureGroupConditionId = UUID.randomUUID().toString();
        String assignmentId = UUID.randomUUID().toString();
        String treatmentId = UUID.randomUUID().toString();
        String assessmentId = UUID.randomUUID().toString();
        String questionId = UUID.randomUUID().toString();
        String integrationClientId = UUID.randomUUID().toString();
        String integrationConfigurationId = UUID.randomUUID().toString();
        String integrationId = UUID.randomUUID().toString();
        String answerMcId = UUID.randomUUID().toString();
        String outcomeId = UUID.randomUUID().toString();

        Export export = Export.builder()
            .experiment(ExperimentExport.builder().id(experimentId).title("source experiment title").participationType(ParticipationTypes.AUTO).build())
            .conditions(List.of(ConditionExport.builder().id(conditionId).name("condition").experimentId(experimentId).build()))
            .exposures(List.of(ExposureExport.builder().id(exposureId).title("exposure").experimentId(experimentId).build()))
            .groups(List.of(GroupExport.builder().id(groupId).name("group").experimentId(experimentId).build()))
            .exposureGroupConditions(List.of(ExposureGroupConditionExport.builder().id(exposureGroupConditionId).exposureId(exposureId).groupId(groupId).conditionId(conditionId).build()))
            .assignments(List.of(AssignmentExport.builder().id(assignmentId).title("assignment").exposureId(exposureId).build()))
            .treatments(List.of(TreatmentExport.builder().id(treatmentId).conditionId(conditionId).assignmentId(assignmentId).build()))
            .assessments(List.of(AssessmentExport.builder().id(assessmentId).title("assessment").treatmentId(treatmentId).build()))
            .questions(List.of(QuestionExport.builder().id(questionId).html("question").questionType(QuestionTypes.MC).assessmentId(assessmentId).questionOrder(1).build()))
            .integrationClients(List.of(IntegrationClientExport.builder().id(integrationClientId).name("integration client").enabled(true).build()))
            .integrationConfigurations(List.of(IntegrationConfigurationExport.builder().id(integrationConfigurationId).clientId(integrationClientId).launchUrl("http://launch.url").build()))
            .integrations(List.of(IntegrationExport.builder().id(integrationId).configurationId(integrationConfigurationId).questionId(questionId).build()))
            .answersMc(List.of(AnswerMcExport.builder().id(answerMcId).answerOrder(1).correct(true).html("answer").questionId(questionId).build()))
            .outcomes(List.of(OutcomeExport.builder().id(outcomeId).title("outcome").maxPoints(10F).exposureId(exposureId).build()))
            .build();

        writeExportJson(export);

        experimentImportService.validate(experimentImport);

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
        export.getConditions().get(0).setExperimentId("999");

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
        export.getExposures().get(0).setExperimentId("999");

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
        export.getGroups().get(0).setExperimentId("999");

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
        export.getExposureGroupConditions().get(0).setExposureId("999");

        assertValidationError(export, "No exposure ID: [999] found for exposureGroupCondition ID: [40]");
    }

    @Test
    void testValidateExposureGroupConditionGroupIdMismatch() throws IOException {
        Export export = fullExport();
        export.getExposureGroupConditions().get(0).setGroupId("999");

        assertValidationError(export, "No group ID: [999] found for exposureGroupCondition ID: [40]");
    }

    @Test
    void testValidateExposureGroupConditionConditionIdMismatch() throws IOException {
        Export export = fullExport();
        export.getExposureGroupConditions().get(0).setConditionId("999");

        assertValidationError(export, "No condition ID: [999] found for exposureGroupCondition ID: [40]");
    }

    @Test
    void testValidateAssignmentExposureIdMismatch() throws IOException {
        Export export = fullExport();
        export.getAssignments().get(0).setExposureId("999");

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
        export.getTreatments().get(0).setConditionId("999");

        assertValidationError(export, "No condition ID: [999] found for treatment ID: [60]");
    }

    @Test
    void testValidateTreatmentAssignmentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getTreatments().get(0).setAssignmentId("999");

        assertValidationError(export, "No assignment ID: [999] found for treatment ID: [60]");
    }

    @Test
    void testValidateAssessmentTreatmentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getAssessments().get(0).setTreatmentId("999");

        assertValidationError(export, "No treatment ID: [999] found for assessment ID: [70]");
    }

    @Test
    void testValidateQuestionAssessmentIdMismatch() throws IOException {
        Export export = fullExport();
        export.getQuestions().get(0).setAssessmentId("999");

        assertValidationError(export, "No assessment ID: [999] found for question ID: [80]");
    }

    @Test
    void testValidateQuestionIntegrationIdMismatch() throws IOException {
        Export export = fullExport();
        export.getQuestions().get(0).setIntegrationId("999");

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
        export.getIntegrationConfigurations().get(0).setClientId("999");

        assertValidationError(export, "No integration client ID: [999] found for integration configuration ID: [91]");
    }

    @Test
    void testValidateIntegrationConfigurationIdMismatch() throws IOException {
        Export export = fullExport();
        export.getIntegrations().get(0).setConfigurationId("999");

        assertValidationError(export, "No integration configuration ID: [999] found for integration ID: [92]");
    }

    @Test
    void testValidateIntegrationQuestionIdMismatch() throws IOException {
        Export export = fullExport();
        export.getIntegrations().get(0).setQuestionId("999");

        assertValidationError(export, "No question ID: [999] found for integration ID: [92]");
    }

    @Test
    void testValidateAnswerMcQuestionIdMismatch() throws IOException {
        Export export = fullExport();
        export.getAnswersMc().get(0).setQuestionId("999");

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
        export.getOutcomes().get(0).setExposureId("999");

        assertValidationError(export, "No exposure ID: [999] found for outcome ID: [94]");
    }

}
