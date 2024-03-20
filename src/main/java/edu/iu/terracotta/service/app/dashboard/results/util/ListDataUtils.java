package edu.iu.terracotta.service.app.dashboard.results.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ListDataUtils {

    public static List<Assessment> findAssessmentsByConditionIdAndExposureIds(long conditionId, List<Long> exposureIds, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment) {
        List<Assessment> assessments = new ArrayList<>();

        exposureIds.forEach(exposureId -> assessments.addAll(findAssessmentsByExposureId(exposureId, experimentAssignments, allAssessmentsByAssignment)));

        return assessments.stream()
            .filter(assessment -> assessment.getTreatment().getCondition().getConditionId() == conditionId)
            .toList();
    }

    public static String findExposureTitleByExposureId(long exposureId, List<Exposure> experimentExposures) {
        Optional<Exposure> exposure = experimentExposures.stream()
            .filter(experimentExposure -> experimentExposure.getExposureId() == exposureId)
            .findFirst();

        if (exposure.isPresent()) {
            return exposure.get().getTitle();
        }

        return String.valueOf(exposureId);
    }

    public static List<Assessment> findAssessmentsByExposureId(long exposureId, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment) {
        List<Assessment> assessments = new ArrayList<>();

        experimentAssignments.stream()
            .filter(experimentAssignment -> experimentAssignment.getExposure().getExposureId() == exposureId)
            .map(experimentAssignment -> experimentAssignment.getAssignmentId())
            .forEach(assignmentId -> assessments.addAll(allAssessmentsByAssignment.get(assignmentId)));

        return assessments;
    }

    public static Optional<ExposureGroupCondition> findExposureGroupConditionByConditionIdAndExposureId(long conditionId, long exposureId, List<ExposureGroupCondition> experimentExposureGroupConditions) {
        return experimentExposureGroupConditions.stream()
            .filter(experimentExposureGroupCondition -> conditionId == experimentExposureGroupCondition.getCondition().getConditionId())
            .filter(experimentExposureGroupCondition -> exposureId == experimentExposureGroupCondition.getExposure().getExposureId())
            .findFirst();
    }

    public static List<Participant> findConsentedParticipantsByGroupId(Long groupId, List<Participant> experimentConsentedParticipants) {
        return experimentConsentedParticipants.stream()
            .filter(participant -> participant.getGroup() != null && participant.getGroup().getGroupId() != null)
            .filter(participant -> groupId.equals(participant.getGroup().getGroupId()))
            .toList();
    }

    public static List<Assessment> findAssessmentsByConditionIdAndExposureId(long conditionId, Long exposureId, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment) {
        return findAssessmentsByExposureId(exposureId, experimentAssignments, allAssessmentsByAssignment).stream()
            .filter(assessment -> assessment.getTreatment().getCondition().getConditionId() == conditionId)
            .toList();
    }

    public static List<Assessment> findAssessmentsByConditionId(Long conditionId, List<Treatment> experimentTreatments) {
        return experimentTreatments.stream()
            .filter(experimentTreatment -> experimentTreatment.getCondition().getConditionId().equals(conditionId))
            .filter(experimentTreatment -> experimentTreatment.getAssessment() != null)
            .map(
                experimentTreatment -> {
                    return experimentTreatment.getAssessment();
                }
            )
            .toList();
    }

    public static List<Assessment> findAssessmentsByTreatmentId(Long treatmentId, Map<Long, List<Assessment>> allAssessmentsByAssignment, List<Treatment> experimentTreatments) {
        return CollectionUtils.emptyIfNull(allAssessmentsByAssignment.get(findAssignmentByTreatmentId(treatmentId, experimentTreatments).get().getAssignmentId())).stream()
            .filter(assessment -> assessment.getTreatment().getTreatmentId().equals(treatmentId))
            .toList();
    }

    public static List<Integer> findAllLmsAssignmentIds(List<Assignment> experimentAssignments) {
        return experimentAssignments.stream()
            .map(experimentAssignment -> Integer.parseInt(experimentAssignment.getLmsAssignmentId()))
            .toList();
    }

    public static Optional<Assignment> findAssignmentByTreatmentId(Long treatmentId, List<Treatment> experimentTreatments) {
        return experimentTreatments.stream()
            .filter(experimentTreatment -> experimentTreatment.getTreatmentId().equals(treatmentId))
            .map(
                experimentTreatment -> experimentTreatment.getAssignment()
            )
            .findFirst();
    }

    public static List<Treatment> findTreatmentsByAssignmentId(Long assignmentId, Map<Long, List<Treatment>> allTreatmentsByAssignment) {
        return MapUtils.getObject(allTreatmentsByAssignment, assignmentId, Collections.emptyList());
    }

    public static int countTreatmentsByAssignmentId(Long assignmentId, Map<Long, List<Treatment>> allTreatmentsByAssignment) {
        return findTreatmentsByAssignmentId(assignmentId, allTreatmentsByAssignment).size();
    }

    public static Optional<AssignmentExtended> findCanvasAssignmentByLmsAssignmentId(String lmsAssignmentId, List<AssignmentExtended> experimentAssignmentsExtended) {
        return experimentAssignmentsExtended.stream()
            .filter(assignmentExtended -> assignmentExtended.getId().equals(Integer.parseInt(lmsAssignmentId)))
            .findFirst();
    }

    public static List<ExposureGroupCondition> findExposureGroupsByConditionId(Long conditionId, List<ExposureGroupCondition> experimentExposureGroupConditions) {
        return experimentExposureGroupConditions.stream()
            .filter(experimentExposureGroupCondition -> conditionId.equals(experimentExposureGroupCondition.getCondition().getConditionId()))
            .toList();
    }

    public static List<Participant> findParticipantsByGroupId(List<Participant> participants, Long groupId) {
        return participants.stream()
            .filter(participant -> participant.getGroup() != null && participant.getGroup().getGroupId() != null)
            .filter(participant -> groupId.equals(participant.getGroup().getGroupId()))
            .toList();
    }

    public static List<Treatment> findTreatmentsByAssignmentId(Long assignmentId, List<Treatment> experimentTreatments) {
        return experimentTreatments.stream()
            .filter(experimentTreatment -> experimentTreatment.getAssignment().getAssignmentId().equals(assignmentId))
            .toList();
    }

    public static List<Submission> findSubmissionsByAssignmentId(Long assignmentId, List<Submission> experimentSubmissions) {
        return experimentSubmissions.stream()
            .filter(experimentSubmission -> experimentSubmission.getAssessment().getTreatment().getAssignment().getAssignmentId().equals(assignmentId))
            .toList();
    }

    public static List<Submission> findSubmissionsByTreatmentId(Long treatmentId, List<Submission> experimentSubmissions) {
        return experimentSubmissions.stream()
            .filter(experimentSubmission -> experimentSubmission.getAssessment().getTreatment().getTreatmentId().equals(treatmentId))
            .toList();
    }

}
