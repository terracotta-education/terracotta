package edu.iu.terracotta.service.app.dashboard.results.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

        when(submissionService.getSubmissions(anyLong(), anyString(), anyLong(), anyBoolean())).thenReturn(Collections.singletonList(submissionDto));

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

}
