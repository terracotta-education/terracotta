package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImportError;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.model.distribute.export.AnswerMcExport;
import edu.iu.terracotta.dao.model.distribute.export.AssessmentExport;
import edu.iu.terracotta.dao.model.distribute.export.AssignmentExport;
import edu.iu.terracotta.dao.model.distribute.export.ConditionExport;
import edu.iu.terracotta.dao.model.distribute.export.ConsentDocumentExport;
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
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import tools.jackson.databind.json.JsonMapper;

class ExperimentImportAsyncServiceImplTest extends BaseTest {

    @InjectMocks private ExperimentImportAsyncServiceImpl experimentImportAsyncServiceImpl;

    private Path importDirectory;
    private List<ExperimentImportError> errors;

    @BeforeEach
    void beforeEach() throws IOException {
        MockitoAnnotations.openMocks(this);
        setup();

        importDirectory = Files.createTempDirectory("experiment-import-test");
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(importDirectory.toFile());
        when(experimentImport.getOwner()).thenReturn(ltiUserEntity);
        when(experimentImport.getContext()).thenReturn(ltiContextEntity);

        // experimentImport is a mock; addErrorMessage() would otherwise be a no-op, so back it
        // with a real mutable list to faithfully reproduce the error-accumulation branching in process().
        errors = new ArrayList<>();
        when(experimentImport.getErrors()).thenReturn(errors);
        doAnswer(
            invocation -> {
                errors.add(mock(ExperimentImportError.class));
                return null;
            }
        ).when(experimentImport).addErrorMessage(anyString());

        when(experimentRepository.save(any(Experiment.class))).thenReturn(experiment);
        when(conditionRepository.save(any(Condition.class))).thenReturn(condition);
        when(exposureRepository.save(any(Exposure.class))).thenReturn(exposure);
        when(groupRepository.save(any(Group.class))).thenReturn(group);
        when(exposureGroupConditionRepository.save(any(ExposureGroupCondition.class))).thenReturn(exposureGroupCondition);
        when(consentDocumentRepository.save(any(ConsentDocument.class))).thenReturn(consentDocument);
        when(integrationClientRepository.save(any(IntegrationClient.class))).thenReturn(integrationClient);
    }

    private ExperimentExport experimentExport() {
        return ExperimentExport.builder()
            .id(100L)
            .title("source experiment title")
            .description("description")
            .exposureType(ExposureTypes.BETWEEN)
            .participationType(ParticipationTypes.AUTO)
            .distributionType(DistributionTypes.EVEN)
            .build();
    }

    private Export fullExport() {
        return Export.builder()
            .experiment(experimentExport())
            .conditions(List.of(ConditionExport.builder().id(300L).name("condition").defaultCondition(true).distributionPct(50F).experimentId(100L).build()))
            .exposures(List.of(ExposureExport.builder().id(400L).title("exposure").experimentId(100L).build()))
            .groups(List.of(GroupExport.builder().id(500L).name("group").experimentId(100L).build()))
            .exposureGroupConditions(List.of(ExposureGroupConditionExport.builder().id(600L).conditionId(300L).exposureId(400L).groupId(500L).build()))
            .assignments(List.of(AssignmentExport.builder().id(700L).title("assignment").exposureId(400L).numOfSubmissions(1).build()))
            .treatments(List.of(TreatmentExport.builder().id(800L).assignmentId(700L).conditionId(300L).build()))
            .assessments(List.of(AssessmentExport.builder().id(900L).title("assessment").treatmentId(800L).build()))
            .questions(
                List.of(
                    QuestionExport.builder().id(1000L).html("mc question").questionType(QuestionTypes.MC).assessmentId(900L).randomizeAnswers(true).build(),
                    QuestionExport.builder().id(1001L).html("essay question").questionType(QuestionTypes.ESSAY).assessmentId(900L).build()
                )
            )
            .integrationClients(List.of(IntegrationClientExport.builder().id(1100L).name("integration client").enabled(true).build()))
            .integrationConfigurations(List.of(IntegrationConfigurationExport.builder().id(1200L).clientId(1100L).launchUrl("http://launch.url").build()))
            .integrations(List.of(IntegrationExport.builder().id(1300L).configurationId(1200L).questionId(1000L).build()))
            .answersMc(List.of(AnswerMcExport.builder().id(1400L).answerOrder(1).correct(true).html("answer").questionId(1000L).build()))
            .outcomes(List.of(OutcomeExport.builder().id(1500L).title("outcome").maxPoints(10F).exposureId(400L).build()))
            .build();
    }

    private void writeExportJson(Export export) throws IOException {
        JsonMapper.builder().build().writeValue(importDirectory.resolve(ExperimentImport.JSON_FILE_NAME).toFile(), export);
    }

    @Test
    void testProcessNoImportDirectory() {
        when(fileStorageService.getExperimentImportFile(anyLong())).thenReturn(null);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        // prepare() records the specific error, then process() itself records a second, generic one
        verify(experimentImport, times(2)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage("No import .zip file found.");
        verify(experimentImport).addErrorMessage("No import file found.");
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessNoJsonFile() {
        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(experimentImport, times(2)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("No JSON file [%s] found in imported .zip file.", ExperimentImport.JSON_FILE_NAME));
        verify(experimentImport).addErrorMessage("No import file found.");
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessMalformedJsonFile() throws IOException {
        Files.writeString(importDirectory.resolve(ExperimentImport.JSON_FILE_NAME), "not valid json");

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(experimentImport, times(2)).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("Error reading JSON file: [%s]", ExperimentImport.JSON_FILE_NAME));
        verify(experimentImport).addErrorMessage("No import file found.");
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessExistingValidationErrors() throws IOException {
        writeExportJson(fullExport());
        errors.add(mock(ExperimentImportError.class));

        // validation errors already present when process() is invoked; it records the error and
        // returns immediately, never reaching the final rollback check that throws the exception
        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(org.mockito.ArgumentMatchers.startsWith("Validation errors:"));
        verify(experimentRepository, never()).save(any(Experiment.class));
    }

    @Test
    void testProcessSuccessNoAssignments() throws AssignmentNotCreatedException, TerracottaConnectorException {
        Export export = fullExport();
        export.setAssignments(Collections.emptyList());
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());

        try {
            writeExportJson(export);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(experimentRepository).save(any(Experiment.class));
        verify(experimentImportRepository).save(experimentImport);
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
        verify(assignmentService, never()).createAssignmentInLms(any(), any(), anyLong(), anyString());
    }

    @Test
    void testProcessSuccessFullExport() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        writeExportJson(fullExport());
        when(assignmentService.createAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString())).thenReturn(assignment);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(conditionRepository).save(any(Condition.class));
        verify(exposureRepository).save(any(Exposure.class));
        verify(groupRepository).save(any(Group.class));
        verify(exposureGroupConditionRepository).save(any(ExposureGroupCondition.class));
        verify(assignmentRepository).save(any(Assignment.class));
        verify(treatmentRepository, times(2)).save(any(edu.iu.terracotta.dao.entity.Treatment.class));
        verify(assessmentRepository).save(any(edu.iu.terracotta.dao.entity.Assessment.class));
        verify(questionRepository).save(any(edu.iu.terracotta.dao.entity.QuestionMc.class));
        // the MC question also matches the broader Question argument matcher, so this is 2 total invocations
        verify(questionRepository, times(2)).save(any(edu.iu.terracotta.dao.entity.Question.class));
        verify(integrationClientRepository).save(any(IntegrationClient.class));
        verify(integrationConfigurationRepository).save(any(edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration.class));
        verify(integrationRepository).save(any(edu.iu.terracotta.dao.entity.integrations.Integration.class));
        verify(answerMcRepository).save(any(edu.iu.terracotta.dao.entity.AnswerMc.class));
        verify(outcomeRepository).save(any(edu.iu.terracotta.dao.entity.Outcome.class));
        verify(assignmentService).createAssignmentInLms(eq(ltiUserEntity), any(Assignment.class), anyLong(), anyString());
        verify(fileStorageService, never()).sendConsentFileToLms(any(), any(), any());
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
        verify(experimentImportRepository).save(experimentImport);
    }

    @Test
    void testProcessIntegrationClientReusesExistingEnabledClient() throws IOException {
        when(integrationClientRepository.findAll()).thenReturn(List.of(integrationClient));
        when(integrationClient.isEnabled()).thenReturn(true);
        when(integrationClient.getName()).thenReturn("integration client");

        Export export = fullExport();
        export.setAssignments(Collections.emptyList());
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(integrationClientRepository, never()).save(any(IntegrationClient.class));
    }

    @Test
    void testProcessConsentParticipationTypeSuccess() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id(200L).title("consent title").html("<p>consent</p>").experimentId(100L).build());
        writeExportJson(export);

        File consentDir = importDirectory.resolve("consent").toFile();
        consentDir.mkdirs();
        Files.writeString(consentDir.toPath().resolve(ExperimentImport.CONSENT_FILE_NAME), "pdf-bytes");

        when(fileStorageService.saveConsentFile(any(), anyString())).thenReturn(
            edu.iu.terracotta.dao.entity.FileSubmissionLocal.builder()
                .encryptionMethod("AES")
                .encryptionPhrase("phrase")
                .filePath("/tmp/consent.pdf")
                .build()
        );
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString())).thenReturn(assignment);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(consentDocumentRepository).save(any(ConsentDocument.class));
        verify(fileStorageService).sendConsentFileToLms(any(ConsentDocument.class), any(Experiment.class), eq(ltiUserEntity));
        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.COMPLETE);
    }

    @Test
    void testProcessConsentParticipationTypeMissingFile() throws IOException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id(200L).title("consent title").html("<p>consent</p>").experimentId(100L).build());
        writeExportJson(export);

        // the missing consent file only aborts the consentDocument() step; process() continues
        // importing every other component and only rolls back once it reaches the final error check
        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo));

        verify(experimentImport).setStatus(edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus.ERROR);
        verify(experimentImport).addErrorMessage(String.format("No consent PDF file [%s] found for experiment with consent participation type.", ExperimentImport.CONSENT_FILE_NAME));
        verify(consentDocumentRepository, never()).save(any(ConsentDocument.class));
        verify(conditionRepository).save(any(Condition.class));
    }

    @Test
    void testProcessConsentFileReadError() throws IOException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id(200L).title("consent title").html("<p>consent</p>").experimentId(100L).build());
        writeExportJson(export);

        File consentDir = importDirectory.resolve("consent").toFile();
        consentDir.mkdirs();
        Files.writeString(consentDir.toPath().resolve(ExperimentImport.CONSENT_FILE_NAME), "pdf-bytes");

        when(fileStorageService.saveConsentFile(any(), anyString())).thenThrow(new RuntimeException("disk full"));

        assertThrows(RuntimeException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo));
    }

    @Test
    void testProcessExperimentTitleCollisionAppendsIndex() throws IOException {
        Export export = fullExport();
        export.setAssignments(Collections.emptyList());
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        String collidingTitle = String.format("%s %s", ExperimentImport.EXPERIMENT_TITLE_PREFIX, export.getExperiment().getTitle());
        when(experimentRepository.existsByTitle(collidingTitle)).thenReturn(true);

        experimentImportAsyncServiceImpl.process(experimentImport, securedInfo);

        verify(experimentImport).setImportedTitle(String.format("%s %s (1)", ExperimentImport.EXPERIMENT_TITLE_PREFIX, export.getExperiment().getTitle()));
    }

    @Test
    void testProcessAssignmentCreationInLmsFailsDeletesCreatedAssignments() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException, AssignmentNotEditedException, ApiException {
        Export export = fullExport();
        export.setAssignments(
            List.of(
                AssignmentExport.builder().id(700L).title("assignment one").exposureId(400L).numOfSubmissions(1).build(),
                AssignmentExport.builder().id(701L).title("assignment two").exposureId(400L).numOfSubmissions(1).build()
            )
        );
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenReturn(assignment)
            .thenThrow(new AssignmentNotCreatedException("failed to create assignment"));

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo));

        assertEquals(String.format("Errors occurred processing experiment import with ID: [%s]. Rolling back transactions.", experimentImport.getId()), exception.getMessage());
        verify(experimentImport).addErrorMessage("Assignment creation in LMS failed");
        verify(assignmentService).deleteAssignmentInLms(eq(assignment), anyString(), eq(ltiUserEntity));
        verify(experimentImportRepository, never()).save(experimentImport);
    }

    @Test
    void testProcessAssignmentDeletionInLmsFailureIsSwallowed() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException, AssignmentNotEditedException, ApiException {
        Export export = fullExport();
        export.setAssignments(
            List.of(
                AssignmentExport.builder().id(700L).title("assignment one").exposureId(400L).numOfSubmissions(1).build(),
                AssignmentExport.builder().id(701L).title("assignment two").exposureId(400L).numOfSubmissions(1).build()
            )
        );
        export.setTreatments(Collections.emptyList());
        export.setAssessments(Collections.emptyList());
        export.setQuestions(Collections.emptyList());
        export.setIntegrations(Collections.emptyList());
        export.setAnswersMc(Collections.emptyList());
        writeExportJson(export);

        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString()))
            .thenReturn(assignment)
            .thenThrow(new TerracottaConnectorException("connector failed"));
        doThrow(new AssignmentNotEditedException("could not delete")).when(assignmentService).deleteAssignmentInLms(any(Assignment.class), anyString(), any());

        assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo));

        verify(assignmentService).deleteAssignmentInLms(eq(assignment), anyString(), eq(ltiUserEntity));
    }

    @Test
    void testProcessConsentAssignmentCreationInLmsFails() throws IOException, AssignmentNotCreatedException, TerracottaConnectorException {
        Export export = fullExport();
        export.getExperiment().setParticipationType(ParticipationTypes.CONSENT);
        export.setConsentDocument(ConsentDocumentExport.builder().id(200L).title("consent title").html("<p>consent</p>").experimentId(100L).build());
        writeExportJson(export);

        File consentDir = importDirectory.resolve("consent").toFile();
        consentDir.mkdirs();
        Files.writeString(consentDir.toPath().resolve(ExperimentImport.CONSENT_FILE_NAME), "pdf-bytes");

        when(fileStorageService.saveConsentFile(any(), anyString())).thenReturn(
            edu.iu.terracotta.dao.entity.FileSubmissionLocal.builder()
                .encryptionMethod("AES")
                .encryptionPhrase("phrase")
                .filePath("/tmp/consent.pdf")
                .build()
        );
        when(assignmentService.createAssignmentInLms(any(), any(), anyLong(), anyString())).thenReturn(assignment);
        doThrow(new IOException("io error")).when(fileStorageService).sendConsentFileToLms(any(ConsentDocument.class), any(Experiment.class), any());

        ExperimentImportException exception = assertThrows(ExperimentImportException.class, () -> experimentImportAsyncServiceImpl.process(experimentImport, securedInfo));

        assertEquals(String.format("Errors occurred processing experiment import with ID: [%s]. Rolling back transactions.", experimentImport.getId()), exception.getMessage());
        verify(experimentImport).addErrorMessage("Consent assignment creation in LMS failed");
    }

}
