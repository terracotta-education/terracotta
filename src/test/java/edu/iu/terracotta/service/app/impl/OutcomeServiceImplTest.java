package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.OutcomeScore;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.OutcomeDto;
import edu.iu.terracotta.dao.model.dto.OutcomePotentialDto;
import edu.iu.terracotta.dao.model.enums.LmsType;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;

@SuppressWarnings("unchecked")
public class OutcomeServiceImplTest extends BaseTest {

    private OutcomeServiceImpl outcomeService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        // apiClient/lmsUtils below also collide with CanvasApiClientImpl/BrightspaceLmsUtilsImpl in
        // BaseServiceTest (see the @InjectMocks pitfall note there), so this class is constructed
        // manually instead of relying on @InjectMocks, which non-deterministically wired the wrong
        // mocks and left apiClient/lmsUtils calls silently unstubbed.
        outcomeService = new OutcomeServiceImpl(
            assignmentRepository,
            experimentRepository,
            exposureRepository,
            ltiUserRepository,
            outcomeRepository,
            outcomeScoreRepository,
            outcomeScoreService,
            participantService,
            apiClient,
            lmsUtils
        );

        when(outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(anyLong(), anyLong(), anyLong())).thenReturn(true);

        when(outcome.getExternal()).thenReturn(true);
        when(outcome.getLmsType()).thenReturn(LmsType.none);
        when(outcomeDto.getOutcomeId()).thenReturn(null);
    }

    @Test
    public void testGetOutcomesForExposure() {
        List<OutcomeDto> retVal = outcomeService.getOutcomesForExposure(1l);

        assertEquals(1, retVal.size());
    }

    @Test
    public void testUpdateOutcomeGrades() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

        verify(outcomeScoreRepository, never()).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoEmail() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(ltiUserEntity.getEmail()).thenReturn(null);

        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

        verify(outcomeScoreRepository, never()).save(any(OutcomeScore.class));
    }

    /*@Test
    public void testUpdateOutcomeGradesNoScores() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());

        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

        verify(outcomeScoreRepository).save(any(OutcomeScore.class));
    }

    @Test
    public void testUpdateOutcomeGradesNoScoresEmailNull() throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());
        when(ltiUserEntity.getEmail()).thenReturn(null);

        outcomeService.updateOutcomeGrades(1l, securedInfo, false);

        verify(outcomeScoreRepository).save(any(OutcomeScore.class));
    }*/

    @Test
    public void testDefaultOutcome() {
        assertDoesNotThrow(() -> {
            outcomeService.defaultOutcome(outcomeDto);
        });
    }

    @Test
    public void testBuildHeaders() {
        HttpHeaders retVal = outcomeService.buildHeaders(UriComponentsBuilder.newInstance(), 0, 0, 0);

        assertNotNull(retVal);
    }

    @Test
    public void testPostOutcome() throws IdInPostException, DataServiceException, TitleValidationException {
        OutcomeDto retVal = outcomeService.postOutcome(outcomeDto, 0);

        assertNotNull(retVal);
    }

    @Test
    public void testGetOutcome() {
        Outcome retVal = outcomeService.getOutcome(0l);

        assertNotNull(retVal);
    }

    @Test
    public void testUpdateOutcome() throws TitleValidationException {
        when(outcome.getExternal()).thenReturn(null);
        outcomeService.updateOutcome(0, outcomeDto);

        verify(outcomeRepository).saveAndFlush(any(Outcome.class));
    }

    @Test
    public void testUpdateOutcomeIsExternal() throws TitleValidationException {
        when(outcome.getExternal()).thenReturn(null);
        when(outcomeDto.getExternal()).thenReturn(true);
        outcomeService.updateOutcome(0, outcomeDto);

        verify(outcomeRepository).saveAndFlush(any(Outcome.class));
    }

    @Test
    public void testPotentialOutcomes() throws DataServiceException, ApiException, TerracottaConnectorException {
        List<OutcomePotentialDto> retVal = outcomeService.potentialOutcomes(0, securedInfo);

        assertNotNull(retVal);
    }

    @Test
    public void testDeleteById() {
        assertDoesNotThrow(() -> {
            outcomeService.deleteById(0);
        });
    }

    @Test
    public void testToDto() {
        OutcomeDto retVal = outcomeService.toDto(outcome, true);

        assertNotNull(retVal);
    }

    @Test
    public void testGetAllByExperiment() {
        List<OutcomeDto> retVal = outcomeService.getAllByExperiment(0);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
    }

    @Test
    public void testPostOutcomeIdInPost() {
        when(outcomeDto.getOutcomeId()).thenReturn(1L);

        assertThrows(IdInPostException.class, () -> outcomeService.postOutcome(outcomeDto, 0));
    }

    @Test
    public void testPostOutcomeExposureNotFound() {
        when(exposureRepository.findById(anyLong())).thenReturn(Optional.empty());

        DataServiceException exception = assertThrows(DataServiceException.class, () -> outcomeService.postOutcome(outcomeDto, 0));
        assertTrue(exception.getMessage().contains("Error 105"));
    }

    @Test
    public void testUpdateOutcomeBlankTitle() {
        when(outcomeDto.getTitle()).thenReturn("");

        assertThrows(TitleValidationException.class, () -> outcomeService.updateOutcome(0, outcomeDto));
    }

    @Test
    public void testUpdateOutcomeTitleTooLong() {
        when(outcomeDto.getTitle()).thenReturn(tooLongTitle());

        assertThrows(TitleValidationException.class, () -> outcomeService.updateOutcome(0, outcomeDto));
    }

    @Test
    public void testDefaultOutcomeTitleTooLong() {
        when(outcomeDto.getTitle()).thenReturn(tooLongTitle());

        assertThrows(TitleValidationException.class, () -> outcomeService.defaultOutcome(outcomeDto));
    }

    @Test
    public void testDefaultOutcomeExternalTrue() {
        when(outcomeDto.getExternal()).thenReturn(true);
        when(outcomeDto.getLmsOutcomeId()).thenReturn("lmsId");
        when(outcomeDto.getLmsType()).thenReturn("CANVAS");

        assertDoesNotThrow(() -> outcomeService.defaultOutcome(outcomeDto));

        verify(outcomeDto, never()).setLmsOutcomeId(null);
    }

    @Test
    public void testPotentialOutcomesAlreadyAssigned() throws DataServiceException, ApiException, TerracottaConnectorException {
        // default setup: assignment.getLmsAssignmentId() == "1" == lmsAssignment.getId() -> already matched, not added
        List<OutcomePotentialDto> retVal = outcomeService.potentialOutcomes(0, securedInfo);

        assertNotNull(retVal);
        assertEquals(0, retVal.size());
    }

    @Test
    public void testPotentialOutcomesNewAssignment() throws DataServiceException, ApiException, TerracottaConnectorException {
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());
        when(consentDocument.getLmsAssignmentId()).thenReturn("999");
        when(lmsAssignment.getSubmissionTypes()).thenReturn(List.of("external_tool"));
        when(lmsAssignment.getPointsPossible()).thenReturn(10F);

        List<OutcomePotentialDto> retVal = outcomeService.potentialOutcomes(0, securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
        assertEquals("1", retVal.get(0).getAssignmentId());
        assertTrue(retVal.get(0).isTerracotta());
    }

    @Test
    public void testFetchSubmissionsForOutcomesNoExternal() throws Exception {
        when(outcome.getExternal()).thenReturn(false);

        Map<String, List<LmsSubmission>> retVal = outcomeService.fetchSubmissionsForOutcomes(List.of(outcome), securedInfo);

        assertNotNull(retVal);
        assertTrue(retVal.isEmpty());
    }

    @Test
    public void testFetchSubmissionsForOutcomesWithResults() throws Exception {
        when(lmsSubmission.getAssignmentId()).thenReturn("1");
        when(apiClient.listSubmissionsForMultipleAssignments(any(), anyString(), any())).thenReturn(List.of(lmsSubmission));

        Map<String, List<LmsSubmission>> retVal = outcomeService.fetchSubmissionsForOutcomes(List.of(outcome), securedInfo);

        assertNotNull(retVal);
        assertEquals(1, retVal.size());
        assertEquals(1, retVal.get("1").size());
    }

    @Test
    public void testUpdateOutcomeGradesNotExternal() throws Exception {
        when(outcome.getExternal()).thenReturn(false);

        outcomeService.updateOutcomeGrades(1L, securedInfo, true);

        verify(participantService, never()).refreshParticipantsIfStale(anyLong());
        verify(outcomeScoreRepository, never()).saveAll(any());
    }

    // updateOutcomeGrades throttles the roster sync (refreshParticipantsIfStale) rather than
    // syncing unconditionally on every gradebook/outcome view.
    @Test
    public void testUpdateOutcomeGradesRefreshesParticipants() throws Exception {
        outcomeService.updateOutcomeGrades(1L, securedInfo, true);

        verify(participantService).refreshParticipantsIfStale(1L);
    }

    @Test
    public void testUpdateOutcomeGradesWithSubmissionsMapMatchesExistingScoreByEmail() throws Exception {
        Map<String, List<LmsSubmission>> submissionsByLmsAssignmentId = new HashMap<>();
        submissionsByLmsAssignmentId.put(outcome.getLmsOutcomeId(), List.of(lmsSubmission));
        when(lmsSubmission.getScore()).thenReturn(95.0);

        outcomeService.updateOutcomeGrades(1L, securedInfo, false, submissionsByLmsAssignmentId);

        verify(apiClient, never()).listSubmissions(any(), any(Outcome.class), any());
        verify(outcomeScore).setScoreNumeric(95.0f);
        verify(outcomeScoreRepository).saveAll(Collections.emptyList());
    }

    @Test
    public void testUpdateOutcomeGradesMatchesExistingScoreBySecondCondition() throws Exception {
        when(apiClient.listSubmissions(any(), any(Outcome.class), anyString())).thenReturn(List.of(lmsSubmission));
        when(ltiUserEntity.getEmail()).thenReturn("someoneelse@example.com");
        when(lmsSubmission.getScore()).thenReturn(null);

        outcomeService.updateOutcomeGrades(1L, securedInfo, false);

        verify(outcomeScore).setScoreNumeric(null);
        verify(outcomeScoreRepository).saveAll(Collections.emptyList());
    }

    @Test
    public void testUpdateOutcomeGradesNoExistingScoreMatchesParticipantByLmsUserId() throws Exception {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());
        when(apiClient.listSubmissions(any(), any(Outcome.class), anyString())).thenReturn(List.of(lmsSubmission));
        when(ltiUserEntity.getLmsUserId()).thenReturn("lms-user-1");
        when(lmsSubmission.getUserId()).thenReturn("lms-user-1");
        when(lmsSubmission.getScore()).thenReturn(88.0);

        outcomeService.updateOutcomeGrades(1L, securedInfo, false);

        ArgumentCaptor<List<OutcomeScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(outcomeScoreRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void testUpdateOutcomeGradesNoExistingScoreMatchesParticipantByEmailAndDisplayName() throws Exception {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());
        when(apiClient.listSubmissions(any(), any(Outcome.class), anyString())).thenReturn(List.of(lmsSubmission));
        when(ltiUserEntity.getLmsUserId()).thenReturn("A");
        when(lmsSubmission.getUserId()).thenReturn("B");

        outcomeService.updateOutcomeGrades(1L, securedInfo, false);

        ArgumentCaptor<List<OutcomeScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(outcomeScoreRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void testUpdateOutcomeGradesNoExistingScoreMatchesParticipantByDisplayNameCaseInsensitive() throws Exception {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());
        when(apiClient.listSubmissions(any(), any(Outcome.class), anyString())).thenReturn(List.of(lmsSubmission));
        when(ltiUserEntity.getLmsUserId()).thenReturn("A");
        when(lmsSubmission.getUserId()).thenReturn("B");
        when(ltiUserEntity.getEmail()).thenReturn(null);
        when(ltiUserEntity.getDisplayName()).thenReturn("John Doe");
        when(lmsSubmission.getUserName()).thenReturn("JOHN DOE");

        outcomeService.updateOutcomeGrades(1L, securedInfo, false);

        ArgumentCaptor<List<OutcomeScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(outcomeScoreRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void testUpdateOutcomeGradesNoMatchAtAll() throws Exception {
        when(outcome.getOutcomeScores()).thenReturn(Collections.emptyList());
        when(apiClient.listSubmissions(any(), any(Outcome.class), anyString())).thenReturn(List.of(lmsSubmission));
        when(ltiUserEntity.getLmsUserId()).thenReturn("A");
        when(lmsSubmission.getUserId()).thenReturn("B");
        when(ltiUserEntity.getEmail()).thenReturn(null);
        when(ltiUserEntity.getDisplayName()).thenReturn("John");
        when(lmsSubmission.getUserName()).thenReturn("Jane");

        outcomeService.updateOutcomeGrades(1L, securedInfo, false);

        verify(outcomeScoreRepository).saveAll(Collections.emptyList());
    }

    private static String tooLongTitle() {
        return "a".repeat(256);
    }


}
