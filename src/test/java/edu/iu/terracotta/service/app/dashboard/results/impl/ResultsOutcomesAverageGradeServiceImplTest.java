package edu.iu.terracotta.service.app.dashboard.results.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import edu.iu.terracotta.BaseTest;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposures;

public class ResultsOutcomesAverageGradeServiceImplTest extends BaseTest {

    @InjectMocks private ResultsOutcomesAverageGradeServiceImpl resultsOutcomesAverageGradeService;

    private Map<Long, List<Assessment>> allAssessmentsByAssignment;
    private Map<Long, List<Treatment>> allTreatmentsByAssignment;
    private List<Assignment> experimentAssignments;
    private List<Participant> experimentConsentedParticipants;
    private List<Exposure> experimentExposures;
    private List<Treatment> experimentTreatments;
    private List<Long> exposureIds;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        setup();

        allAssessmentsByAssignment = Collections.singletonMap(1L, Collections.singletonList(assessment));
        allTreatmentsByAssignment = Collections.singletonMap(1L, Collections.singletonList(treatment));
        experimentAssignments = Collections.singletonList(assignment);
        experimentConsentedParticipants = Collections.singletonList(participant);
        experimentExposures = Collections.singletonList(exposure);
        experimentTreatments = Collections.singletonList(treatment);
        exposureIds = Collections.singletonList(1L);

        when(experiment.getConditions()).thenReturn(Arrays.asList(condition, condition));
    }

    @Test
    void testConditions() {
        OutcomesConditions ret = resultsOutcomesAverageGradeService.conditions(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, allTreatmentsByAssignment, experimentTreatments);

        assertNotNull(ret);
        assertEquals(3, ret.getRows().size());
    }

    @Test
    void testConditionsNoScores() {
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);

        OutcomesConditions ret = resultsOutcomesAverageGradeService.conditions(experiment, exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, allTreatmentsByAssignment, experimentTreatments);

        assertNotNull(ret);
        assertEquals(3, ret.getRows().size());
    }

    @Test
    void testExposures() {
        OutcomesExposures ret = resultsOutcomesAverageGradeService.exposures(exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, experimentExposures);

        assertNotNull(ret);
        assertEquals(2, ret.getRows().size());
    }

    @Test
    void testExposuresNoScores() {
        when(submissionService.getScoreFromMultipleSubmissions(any(Participant.class), any(Assessment.class))).thenReturn(null);

        OutcomesExposures ret = resultsOutcomesAverageGradeService.exposures(exposureIds, experimentAssignments, allAssessmentsByAssignment, experimentConsentedParticipants, experimentExposures);

        assertNotNull(ret);
        assertEquals(2, ret.getRows().size());
    }

}
