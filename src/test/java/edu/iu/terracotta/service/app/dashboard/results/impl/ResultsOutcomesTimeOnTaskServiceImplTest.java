package edu.iu.terracotta.service.app.dashboard.results.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposureOverall;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposures;
import edu.iu.terracotta.exceptions.NoSubmissionsException;

public class ResultsOutcomesTimeOnTaskServiceImplTest extends BaseTest {

    @InjectMocks private ResultsOutcomesTimeOnTaskServiceImpl resultsOutcomesTimeOnTaskService;

    private List<Long> exposureIds;
    private List<Assignment> experimentAssignments;
    private List<Exposure> experimentExposures;
    private List<Treatment> experimentTreatments;
    private Map<Long, List<Assessment>> allAssessmentsByAssignment;
    private Map<Long, List<Treatment>> allTreatmentsByAssignment;
    private List<Participant> experimentConsentedParticipants;

    @BeforeEach
    public void beforeEach() throws NoSubmissionsException {
        MockitoAnnotations.openMocks(this);

        setup();

        allAssessmentsByAssignment = Collections.singletonMap(1L, Collections.singletonList(assessment));
        allTreatmentsByAssignment = Collections.singletonMap(1L, Collections.singletonList(treatment));
        experimentAssignments = Collections.singletonList(assignment);
        experimentConsentedParticipants = Collections.singletonList(participant);
        experimentExposures = Collections.singletonList(exposure);
        experimentTreatments = Collections.singletonList(treatment);
        exposureIds = Collections.singletonList(1L);

        when(submissionService.getSubmissions(anyLong(), any(), anyLong(), anyBoolean())).thenReturn(Collections.singletonList(submissionDto));

        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
    }

    @Test
    void testConditions() {
        OutcomesConditions ret = resultsOutcomesTimeOnTaskService.conditions(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, allTreatmentsByAssignment, experimentTreatments);

        assertNotNull(ret);
        assertEquals(3, ret.getRows().size());
    }

    @Test
    void testConditionsNoScores() {
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);

        OutcomesConditions ret = resultsOutcomesTimeOnTaskService.conditions(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, allTreatmentsByAssignment, experimentTreatments);

        assertNotNull(ret);
        assertEquals(3, ret.getRows().size());
    }

    @Test
    void testExposures() {
        OutcomesExposures ret = resultsOutcomesTimeOnTaskService.exposures(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, experimentExposures);

        assertNotNull(ret);
        assertEquals(2, ret.getRows().size());
    }

    @Test
    void testExposuresNoScores() {
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);

        OutcomesExposures ret = resultsOutcomesTimeOnTaskService.exposures(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, experimentExposures);

        assertNotNull(ret);
        assertEquals(2, ret.getRows().size());
    }

    @Test
    void testConditionsFetchesSubmissionsOncePerAssessmentRegardlessOfParticipantCount() {
        resultsOutcomesTimeOnTaskService.conditions(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, Collections.singletonList(participant), allTreatmentsByAssignment, experimentTreatments);
        int callsForOneParticipant = mockingDetails(submissionService).getInvocations().size();

        clearInvocations(submissionService);

        List<Participant> multipleParticipants = Arrays.asList(participant, mock(Participant.class), mock(Participant.class));
        resultsOutcomesTimeOnTaskService.conditions(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, multipleParticipants, allTreatmentsByAssignment, experimentTreatments);
        int callsForThreeParticipants = mockingDetails(submissionService).getInvocations().size();

        // submissions are fetched once per assessment instead of once per (participant, assessment) pair
        assertEquals(callsForOneParticipant, callsForThreeParticipants);
    }

    @Test
    void testExposuresFetchesSubmissionsOncePerAssessmentRegardlessOfParticipantCount() {
        resultsOutcomesTimeOnTaskService.exposures(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, Collections.singletonList(participant), experimentExposures);
        int callsForOneParticipant = mockingDetails(submissionService).getInvocations().size();

        clearInvocations(submissionService);

        List<Participant> multipleParticipants = Arrays.asList(participant, mock(Participant.class), mock(Participant.class));
        resultsOutcomesTimeOnTaskService.exposures(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, multipleParticipants, experimentExposures);
        int callsForThreeParticipants = mockingDetails(submissionService).getInvocations().size();

        assertEquals(callsForOneParticipant, callsForThreeParticipants);
    }

    @Test
    void testConditionsNoSubmissionsExceptionYieldsZeroedScores() throws NoSubmissionsException {
        when(submissionService.getSubmissions(anyLong(), any(), anyLong(), anyBoolean())).thenThrow(new NoSubmissionsException("no submissions"));

        OutcomesConditions ret = resultsOutcomesTimeOnTaskService.conditions(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, allTreatmentsByAssignment, experimentTreatments);

        assertNotNull(ret);
        assertEquals(3, ret.getRows().size());

        ret.getRows().forEach(
            row -> {
                assertEquals(0d, row.getMean());
                assertEquals(0L, row.getNumber());
                assertEquals(0d, row.getStandardDeviation());
                assertTrue(row.getScores().isEmpty());
            }
        );
    }

    @Test
    void testExposuresNoSubmissionsExceptionYieldsZeroedScoresAndOverallRow() throws NoSubmissionsException {
        when(submissionService.getSubmissions(anyLong(), any(), anyLong(), anyBoolean())).thenThrow(new NoSubmissionsException("no submissions"));

        OutcomesExposures ret = resultsOutcomesTimeOnTaskService.exposures(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, experimentExposures);

        assertNotNull(ret);
        assertEquals(2, ret.getRows().size());

        ret.getRows().forEach(
            row -> {
                assertEquals(0d, row.getMean());
                assertEquals(0L, row.getNumber());
                assertEquals(0d, row.getStandardDeviation());
            }
        );

        assertTrue(ret.getRows().stream().anyMatch(row -> OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE.equals(row.getTitle())));
    }

}
