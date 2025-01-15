package edu.iu.terracotta.service.app.dashboard.results;

import java.util.List;
import java.util.Map;

import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposures;


public interface ResultsOutcomesAverageGradeService {

    /**
     * Create the "Conditions" outcomes data area.
     *
     * @param experiment
     * @param exposureIds
     * @param experimentAssignments
     * @param allAssessmentsByAssignment
     * @param experimentConsentedParticipants
     * @param allTreatmentsByAssignment
     * @param experimentTreatments
     * @return
     */
    OutcomesConditions conditions(Experiment experiment, List<Long> exposureIds, List<Assignment> experimentAssignments, Map<Long,
        List<Assessment>> allAssessmentsByAssignment, List<Participant> experimentConsentedParticipants, Map<Long, List<Treatment>> allTreatmentsByAssignment,
        List<Treatment> experimentTreatments);

    /**
     * Create the "Exposures" outcomes data area.
     *
     * @param exposureIds
     * @param experimentAssignments
     * @param allAssessmentsByAssignment
     * @param experimentConsentedParticipants
     * @param experimentExposures
     * @return
     */
    OutcomesExposures exposures(List<Long> exposureIds, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment,
        List<Participant> experimentConsentedParticipants, List<Exposure> experimentExposures);

}
