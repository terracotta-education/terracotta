package edu.iu.terracotta.service.app.async.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.exceptions.DataServiceException;

public class AssignmentAsyncServiceImplTest extends BaseTest {

    @Mock private AssignmentFileArchiveRepository assignmentFileArchiveRepository;
    @Mock private Assignment assignment2;
    @Mock private LmsAssignment lmsAssignment2;
    @Mock private LmsExternalToolFields lmsExternalToolFields2;
    @Mock private Participant participant2;

    private AssignmentAsyncServiceImpl assignmentAsyncService;

    @TempDir private Path tempDir;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // constructed manually rather than via @InjectMocks: ApiClient is ambiguous against
        // the canvasApiClient mock (also assignable to ApiClient), causing @InjectMocks to
        // nondeterministically wire the wrong mock into the constructor.
        assignmentAsyncService = new AssignmentAsyncServiceImpl(
            answerFileSubmissionRepository,
            assignmentFileArchiveRepository,
            assignmentRepository,
            experimentRepository,
            ltiContextRepository,
            ltiUserRepository,
            obsoleteAssignmentRepository,
            participantRepository,
            treatmentRepository,
            assignmentService,
            fileStorageService,
            apiClient
        );

        ReflectionTestUtils.setField(assignmentAsyncService, "assignmentFileArchiveLocalPathRoot", tempDir.toString());
    }

    // handleAssignmentTasksInLmsByContext

    @Test
    void testHandleAssignmentTasksInLmsByContext() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        assignmentAsyncService.handleAssignmentTasksInLmsByContext(securedInfo);

        verify(assignmentService).getAllAssignmentsForLmsCourse(securedInfo);
        verify(assignmentRepository, times(2)).findAssignmentsToCheckByContext(securedInfo.getContextId());
    }

    @Test
    void testHandleAssignmentTasksInLmsByContextPropagatesException() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(assignmentService.getAllAssignmentsForLmsCourse(any())).thenThrow(new ApiException("lms error"));

        assertThrows(ApiException.class, () -> assignmentAsyncService.handleAssignmentTasksInLmsByContext(securedInfo));
    }

    // checkAndRestoreAssignmentsInLmsByContext

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextNoAssignmentsToCheck() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignmentRepository.findAssignmentsToCheckByContext(anyLong())).thenReturn(Collections.emptyList());

        assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(assignmentService, never()).restoreAssignmentInLms(any(Assignment.class));
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextRecreatesMissingAssignment() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("does-not-match");

        assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(assignmentService).restoreAssignmentInLms(assignment);
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextNullLmsAssignmentIdRecreates() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignment.getLmsAssignmentId()).thenReturn(null);

        assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(assignmentService).restoreAssignmentInLms(assignment);
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextRecreateExceptionSwallowed() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("does-not-match");
        doThrow(new ApiException("could not restore")).when(assignmentService).restoreAssignmentInLms(any(Assignment.class));

        assertDoesNotThrow(() -> assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment)));

        verify(assignmentService).restoreAssignmentInLms(assignment);
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextPerItemFailureDoesNotAbortBatch() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignment.getAssignmentId()).thenReturn(1L);
        when(assignment.getLmsAssignmentId()).thenReturn("missing-1");
        when(assignment2.getAssignmentId()).thenReturn(2L);
        when(assignment2.getLmsAssignmentId()).thenReturn("missing-2");
        when(assignmentRepository.findAssignmentsToCheckByContext(anyLong())).thenReturn(List.of(assignment, assignment2));
        doThrow(new ApiException("failure on first")).when(assignmentService).restoreAssignmentInLms(assignment);

        assertDoesNotThrow(() -> assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment)));

        verify(assignmentService).restoreAssignmentInLms(assignment);
        verify(assignmentService).restoreAssignmentInLms(assignment2);
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextUpdatesMetadataWhenPresent() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignment.getLmsAssignmentId()).thenReturn("1");
        when(assignment.getMetadata()).thenReturn("some-metadata");
        when(lmsAssignment.getId()).thenReturn("1");

        assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(assignmentService, never()).restoreAssignmentInLms(any(Assignment.class));
        verify(apiClient).updateAssignmentMetadata(assignment, lmsAssignment);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextSkipsMetadataUpdateWhenBlank() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignment.getLmsAssignmentId()).thenReturn("1");
        when(assignment.getMetadata()).thenReturn(null);
        when(lmsAssignment.getId()).thenReturn("1");

        assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(apiClient, never()).updateAssignmentMetadata(any(Assignment.class), any(LmsAssignment.class));
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void testCheckAndRestoreAssignmentsInLmsByContextMetadataUpdateExceptionSwallowed() throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        when(assignment.getLmsAssignmentId()).thenReturn("1");
        when(assignment.getMetadata()).thenReturn("some-metadata");
        when(lmsAssignment.getId()).thenReturn("1");
        doThrow(new TerracottaConnectorException("could not update metadata")).when(apiClient).updateAssignmentMetadata(any(Assignment.class), any(LmsAssignment.class));

        assertDoesNotThrow(() -> assignmentAsyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment)));

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    // handleObsoleteAssignmentsInLmsByContext

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextNoLmsAssignments() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of());

        verify(ltiContextRepository, never()).findById(anyLong());
        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextMarksObsoleteAssignment() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn(LTI_URL + "?experiment=99&assignment=1");

        assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(apiClient).editAssignment(any(LtiUserEntity.class), eq(lmsAssignment), anyString());
        verify(obsoleteAssignmentRepository).save(any(ObsoleteAssignment.class));
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextSkipsWhenExperimentStillInContext() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn(LTI_URL + "?experiment=1&assignment=1");

        assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
        verify(obsoleteAssignmentRepository, never()).save(any(ObsoleteAssignment.class));
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextSkipsWhenNoQueryParameters() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn(LTI_URL);

        assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextSkipsWhenNoExperimentParam() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn(LTI_URL + "?assignment=1");

        assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextSkipsWhenUrlNotLocal() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn("http://some-other-host.example?experiment=99&assignment=1");

        assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextAlreadyConverted() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn(LTI_URL + "?experiment=99&assignment=1");
        when(obsoleteAssignment.getLmsAssignmentId()).thenReturn("2");

        assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment));

        verify(apiClient, never()).editAssignment(any(LtiUserEntity.class), any(LmsAssignment.class), anyString());
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextPerItemFailureDoesNotAbortBatch() throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn(LTI_URL + "?experiment=97&assignment=1");
        when(lmsAssignment2.getId()).thenReturn("3");
        when(lmsAssignment2.getLmsExternalToolFields()).thenReturn(lmsExternalToolFields2);
        when(lmsExternalToolFields2.getUrl()).thenReturn(LTI_URL + "?experiment=98&assignment=2");

        doThrow(new ApiException("failure editing first")).when(apiClient).editAssignment(any(LtiUserEntity.class), eq(lmsAssignment), anyString());

        assertDoesNotThrow(() -> assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment, lmsAssignment2)));

        verify(apiClient).editAssignment(any(LtiUserEntity.class), eq(lmsAssignment), anyString());
        verify(apiClient).editAssignment(any(LtiUserEntity.class), eq(lmsAssignment2), anyString());
        verify(obsoleteAssignmentRepository, times(1)).save(any(ObsoleteAssignment.class));
    }

    @Test
    void testHandleObsoleteAssignmentsInLmsByContextLtiContextNotFoundThrows() {
        when(lmsAssignment.getId()).thenReturn("2");
        when(lmsExternalToolFields.getUrl()).thenReturn(LTI_URL + "?experiment=99&assignment=1");
        when(ltiContextRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(
            DataServiceException.class,
            () -> assignmentAsyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo, List.of(lmsAssignment))
        );
    }

    // processAssignmentFileArchive

    @Test
    void testProcessAssignmentFileArchiveNoFileQuestionsAborts() throws IOException {
        AssignmentFileArchive archive = buildAssignmentFileArchive();

        assignmentAsyncService.processAssignmentFileArchive(archive);

        verify(participantRepository, never()).findByExperiment_ExperimentId(anyLong());
        verify(assignmentFileArchiveRepository, never()).save(any(AssignmentFileArchive.class));
    }

    @Test
    void testProcessAssignmentFileArchiveNoFilesResultsInErrorStatus() throws IOException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(question.getHtml()).thenReturn("<p>Upload your file</p>");
        when(answerFileSubmissionRepository.findByQuestionSubmission_Question_QuestionId(anyLong())).thenReturn(Collections.emptyList());

        AssignmentFileArchive archive = buildAssignmentFileArchive();

        assignmentAsyncService.processAssignmentFileArchive(archive);

        assertEquals(AssignmentFileArchiveStatus.ERROR, archive.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    void testProcessAssignmentFileArchiveRenameFailureLoggedAndResultsInErrorStatus() throws IOException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(question.getHtml()).thenReturn("<p>Upload your file</p>");

        AnswerFileSubmission answerFileSubmission = AnswerFileSubmission.builder()
            .answerFileSubmissionId(1L)
            .fileName("renamed.txt")
            .questionSubmission(questionSubmission)
            .build();

        when(answerFileSubmissionRepository.findByQuestionSubmission_Question_QuestionId(anyLong())).thenReturn(List.of(answerFileSubmission));
        when(fileStorageService.getFileSubmissionLocal(1L)).thenReturn(new File(tempDir.toFile(), "does-not-exist/original.dat"));

        AssignmentFileArchive archive = buildAssignmentFileArchive();

        assertDoesNotThrow(() -> assignmentAsyncService.processAssignmentFileArchive(archive));

        assertEquals(AssignmentFileArchiveStatus.ERROR, archive.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    void testProcessAssignmentFileArchiveSuccessResultsInReadyStatus() throws IOException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(question.getHtml()).thenReturn("<p>Upload your file</p>");

        Path sourceFile = tempDir.resolve("original.dat");
        Files.writeString(sourceFile, "file contents");

        AnswerFileSubmission answerFileSubmission = AnswerFileSubmission.builder()
            .answerFileSubmissionId(1L)
            .fileName("renamed.txt")
            .questionSubmission(questionSubmission)
            .build();

        when(answerFileSubmissionRepository.findByQuestionSubmission_Question_QuestionId(anyLong())).thenReturn(List.of(answerFileSubmission));
        when(fileStorageService.getFileSubmissionLocal(1L)).thenReturn(sourceFile.toFile());
        when(fileStorageService.compressDirectory(anyString(), eq(""), eq(AssignmentFileArchive.COMPRESSED_FILE_EXTENSION), eq(false))).thenReturn(true);

        AssignmentFileArchive archive = buildAssignmentFileArchive();

        assignmentAsyncService.processAssignmentFileArchive(archive);

        assertEquals(AssignmentFileArchiveStatus.READY, archive.getStatus());
        verify(fileStorageService).compressDirectory(anyString(), eq(""), eq(AssignmentFileArchive.COMPRESSED_FILE_EXTENSION), eq(false));
        verify(fileStorageService).saveAssignmentFileArchive(eq(archive), any(File.class));
        verify(assignmentFileArchiveRepository).save(archive);
    }

    @Test
    void testProcessAssignmentFileArchiveNoConsentingParticipantsThrowsNpe() throws IOException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(question.getHtml()).thenReturn("<p>Upload your file</p>");
        when(participant.getConsent()).thenReturn(false);

        Path sourceFile = tempDir.resolve("original.dat");
        Files.writeString(sourceFile, "file contents");

        AnswerFileSubmission answerFileSubmission = AnswerFileSubmission.builder()
            .answerFileSubmissionId(1L)
            .fileName("renamed.txt")
            .questionSubmission(questionSubmission)
            .build();

        when(answerFileSubmissionRepository.findByQuestionSubmission_Question_QuestionId(anyLong())).thenReturn(List.of(answerFileSubmission));
        when(fileStorageService.getFileSubmissionLocal(1L)).thenReturn(sourceFile.toFile());

        AssignmentFileArchive archive = buildAssignmentFileArchive();

        assertThrows(NullPointerException.class, () -> assignmentAsyncService.processAssignmentFileArchive(archive));
    }

    @Test
    void testProcessAssignmentFileArchiveDuplicateParticipantNamesDoNotHang() throws IOException {
        when(question.getQuestionType()).thenReturn(QuestionTypes.FILE);
        when(question.getHtml()).thenReturn("<p>Upload your file</p>");
        when(participant2.getConsent()).thenReturn(true);
        when(participant2.getParticipantId()).thenReturn(2L);
        when(participant2.getLtiUserEntity()).thenReturn(ltiUserEntity);
        when(participantRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(List.of(participant, participant2));
        when(answerFileSubmissionRepository.findByQuestionSubmission_Question_QuestionId(anyLong())).thenReturn(Collections.emptyList());

        AssignmentFileArchive archive = buildAssignmentFileArchive();

        assignmentAsyncService.processAssignmentFileArchive(archive);

        assertEquals(AssignmentFileArchiveStatus.ERROR, archive.getStatus());
        verify(assignmentFileArchiveRepository).save(archive);
    }

    private AssignmentFileArchive buildAssignmentFileArchive() {
        AssignmentFileArchive archive = AssignmentFileArchive.builder()
            .assignment(assignment)
            .fileUri("some/file/uri")
            .build();
        archive.setUuid(UUID.randomUUID());
        archive.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        return archive;
    }

}
