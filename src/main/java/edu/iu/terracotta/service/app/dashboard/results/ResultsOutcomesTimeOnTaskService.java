package edu.iu.terracotta.service.app.dashboard.results;

import java.util.List;
import java.util.Map;

import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposures;

public interface ResultsOutcomesTimeOnTaskService {

    /**
     * Create the "Conditions" outcomes data area.
     *
     * @param experiment
     * @param exposureIds
     * @param experimentAssignments
     * @param allAssessmentsByAssignment
     * @param experimentConsentedParticipants
     * @param allTreatmentsByAssignment
     * @return
     */
    OutcomesConditions conditions(Experiment experiment, List<Long> exposureIds, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment, List<Participant> experimentConsentedParticipants, Map<Long, List<Treatment>> allTreatmentsByAssignment, List<Treatment> experimentTreatments);

    /**
     * Create the "Exposures" outcomes data area.
     *
     * @param experiment
     * @param exposureIds
     * @param experimentAssignments
     * @param allAssessmentsByAssignment
     * @param experimentConsentedParticipants
     * @param experimentExposures
     * @return
     */
    OutcomesExposures exposures(Experiment experiment, List<Long> exposureIds, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment, List<Participant> experimentConsentedParticipants, List<Exposure> experimentExposures);

}
