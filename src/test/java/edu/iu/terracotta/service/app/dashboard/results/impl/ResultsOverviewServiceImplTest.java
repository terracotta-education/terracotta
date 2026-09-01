package edu.iu.terracotta.service.app.dashboard.results.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.base.BaseTest;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.ResultsOverviewDto;
import edu.iu.terracotta.dao.model.enums.ExposureTypes;

public class ResultsOverviewServiceImplTest extends BaseTest {

    @InjectMocks private ResultsOverviewServiceImpl resultsOverviewService;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        when(assessmentRepository.findByTreatment_Assignment_AssignmentId(anyLong())).thenReturn(Arrays.asList(assessment, assessment));
        when(assessmentRepository.findByTreatment_Assignment_AssignmentIdIn(any())).thenReturn(Arrays.asList(assessment, assessment));
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Arrays.asList(assignment, assignment));
        when(exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(exposureGroupCondition));
        when(participantRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.singletonList(participant));
        when(submissionRepository.countByAssessment_AssessmentId(anyLong())).thenReturn(1L);
        when(submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(anyLong())).thenReturn(1L);
        when(treatmentRepository.findByCondition_Experiment_ExperimentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Arrays.asList(treatment, treatment));

        when(assessmentSubmissionService.calculateMaxScore(any(Assessment.class))).thenReturn(1F);
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(1F);
        when(submissionService.getScoresFromMultipleSubmissions(any(), any(Assessment.class))).thenReturn(Collections.singletonMap(1L, 1F));

        when(assessment.getQuestions()).thenReturn(Collections.emptyList());
        when(experiment.getExposureType()).thenReturn(ExposureTypes.BETWEEN);
    }

    @Test
    void testOverview() {
        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(1.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(3, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(0.0, ret.getConditions().getRows().get(1).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(1).getSubmissionCount());
        assertEquals(4.0, ret.getConditions().getRows().get(2).getSubmissionRate());
        assertEquals(4, ret.getConditions().getRows().get(2).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(1.0, ret.getParticipants().getConsentRate());
        assertEquals(1, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewSingleConditionExperiment() {
        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(1.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(1.0, ret.getParticipants().getConsentRate());
        assertEquals(1, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewSingleCondition() {
        when(treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(anyLong())).thenReturn(Arrays.asList(treatment, treatment));
        when(treatmentRepository.findByAssignment_AssignmentIdInOrderByCondition_ConditionIdAsc(any())).thenReturn(Arrays.asList(treatment, treatment));
        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(1.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(1.0, ret.getParticipants().getConsentRate());
        assertEquals(1, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewNoAssignments() {
        when(assignmentRepository.findByExposure_Experiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNull(ret.getAssignments().getRows());

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(1.0, ret.getParticipants().getConsentRate());
        assertEquals(1, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewNoAssignmentScores() {
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);
        when(submissionService.getScoresFromMultipleSubmissions(any(), any(Assessment.class))).thenReturn(Collections.emptyMap());

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(0.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(1.0, ret.getParticipants().getConsentRate());
        assertEquals(1, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewNoConsentedParticipants() {
        when(participant.getConsent()).thenReturn(false);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(0.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(0.0, ret.getParticipants().getConsentRate());
        assertEquals(0, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewNoParticipants() {
        when(participantRepository.findByExperiment_ExperimentId(anyLong())).thenReturn(Collections.emptyList());

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(0.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(0, ret.getParticipants().getClassEnrollment());
        assertEquals(0.0, ret.getParticipants().getConsentRate());
        assertEquals(0, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewOnlyTestStudentParticipants() {
        when(participant.isTestStudent()).thenReturn(true);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(0.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(0, ret.getParticipants().getClassEnrollment());
        assertEquals(0.0, ret.getParticipants().getConsentRate());
        assertEquals(0, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewNoConditions() {
        when(experiment.getConditions()).thenReturn(Collections.emptyList());

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(1.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNull(ret.getConditions().getRows());
        assertNull(ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(1.0, ret.getParticipants().getConsentRate());
        assertEquals(1, ret.getParticipants().getCount());
    }

    @Test
    void testOverviewNoSubmissions() {
        when(submissionRepository.countByAssessment_AssessmentId(anyLong())).thenReturn(0L);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());

        ret.getAssignments().getRows()
            .forEach(row -> {
                assertEquals(1.0, row.getAverageGrade());
                assertEquals(0.0, row.getStandardDeviation());
            }
        );

        assertNotNull(ret.getConditions());
        assertNotNull(ret.getConditions().getRows());
        assertEquals(1, ret.getConditions().getRows().size());
        assertEquals(0.0, ret.getConditions().getRows().get(0).getSubmissionRate());
        assertEquals(0, ret.getConditions().getRows().get(0).getSubmissionCount());
        assertEquals(ExposureTypes.BETWEEN, ret.getConditions().getExposureType());

        assertNotNull(ret.getParticipants());
        assertEquals(1, ret.getParticipants().getClassEnrollment());
        assertEquals(1.0, ret.getParticipants().getConsentRate());
        assertEquals(1, ret.getParticipants().getCount());
    }

    @Test
    void testCalculateOpenAssignmentLockNullUnlockNullTrue() {
        when(lmsAssignment.getLockAt()).thenReturn(null);
        when(lmsAssignment.getUnlockAt()).thenReturn(null);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertTrue(ret.getAssignments().getRows().get(0).isOpen());
    }

    @Test
    void testCalculateOpenAssignmentUnlockNullTrue() {
        Date lock = new Date();
        lock.setTime(lock.getTime() + (60 * 1000)); // add 1 minute in future
        when(lmsAssignment.getLockAt()).thenReturn(lock);
        when(lmsAssignment.getUnlockAt()).thenReturn(null);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertTrue(ret.getAssignments().getRows().get(0).isOpen());
    }

    @Test
    void testCalculateOpenAssignmentUnlockNullFalse() {
        Date lock = new Date();
        lock.setTime(lock.getTime() - (60 * 1000)); // add 1 minute in past
        when(lmsAssignment.getLockAt()).thenReturn(lock);
        when(lmsAssignment.getUnlockAt()).thenReturn(null);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertFalse(ret.getAssignments().getRows().get(0).isOpen());
    }

    @Test
    void testCalculateOpenAssignmentLockNullTrue() {
        Date unlock = new Date();
        unlock.setTime(unlock.getTime() - (60 * 1000)); // add 1 minute in past
        when(lmsAssignment.getLockAt()).thenReturn(null);
        when(lmsAssignment.getUnlockAt()).thenReturn(unlock);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertTrue(ret.getAssignments().getRows().get(0).isOpen());
    }

    @Test
    void testCalculateOpenAssignmentLockNullFalse() {
        Date unlock = new Date();
        unlock.setTime(unlock.getTime() + (60 * 1000)); // add 1 minute in future
        when(lmsAssignment.getLockAt()).thenReturn(null);
        when(lmsAssignment.getUnlockAt()).thenReturn(unlock);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertFalse(ret.getAssignments().getRows().get(0).isOpen());
    }

    @Test
    void testCalculateOpenAssignmentTrue() {
        Date unlock = new Date();
        unlock.setTime(unlock.getTime() - (60 * 1000)); // add 1 minute in past
        Date lock = new Date();
        lock.setTime(lock.getTime() + (60 * 1000)); // add 1 minute in future
        when(lmsAssignment.getLockAt()).thenReturn(lock);
        when(lmsAssignment.getUnlockAt()).thenReturn(unlock);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertTrue(ret.getAssignments().getRows().get(0).isOpen());
    }

    @Test
    void testCalculateOpenAssignmentFalse() {
        Date unlock = new Date();
        unlock.setTime(unlock.getTime() + (60 * 1000)); // add 1 minute in future
        Date lock = new Date();
        lock.setTime(lock.getTime() - (60 * 1000)); // add 1 minute in past
        when(lmsAssignment.getLockAt()).thenReturn(lock);
        when(lmsAssignment.getUnlockAt()).thenReturn(unlock);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertFalse(ret.getAssignments().getRows().get(0).isOpen());
    }

    // LMS assignment metadata (open/lock dates) must come from one course-wide LMS call, not one
    // call per assignment - each is a real network round trip to Canvas/Brightspace
    @Test
    void testOverviewFetchesLmsAssignmentsInOneBatchedCall() throws Exception {
        resultsOverviewService.overview(experiment, securedInfo);

        verify(assignmentService, times(1)).getAllAssignmentsForLmsCourse(securedInfo);
        verify(assignmentService, never()).getLmsAssignmentById(anyString(), any());
    }

    // a failure reaching the LMS (e.g. a rejected OAuth token) shouldn't take down the whole
    // overview - the assignments themselves already came from our own DB
    @Test
    void testOverviewLmsFailureStillReturnsAssignments() throws Exception {
        doThrow(new ApiException("Failed to get the list of assignments Canvas course")).when(assignmentService).getAllAssignmentsForLmsCourse(securedInfo);

        ResultsOverviewDto ret = resultsOverviewService.overview(experiment, securedInfo);

        assertNotNull(ret);
        assertNotNull(ret.getAssignments());
        assertNotNull(ret.getAssignments().getRows());
        assertEquals(3, ret.getAssignments().getRows().size());
        // no LMS assignment found for any of them -> treated as open by default
        assertTrue(ret.getAssignments().getRows().get(0).isOpen());
    }

}
