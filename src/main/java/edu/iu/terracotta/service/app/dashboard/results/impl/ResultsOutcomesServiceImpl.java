package edu.iu.terracotta.service.app.dashboard.results.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.ResultsOutcomesDto;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition.OutcomesCondition;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.condition.OutcomesCondition.OutcomesConditionBuilder;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.enums.AlternateIdType;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.enums.OutcomeType;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposure;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposureOverall;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposures;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposure.OutcomesExposureBuilder;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.exposure.OutcomesExposureOverall.OutcomesExposureOverallBuilder;
import edu.iu.terracotta.model.app.dto.dashboard.results.outcomes.request.ResultsOutcomesRequestDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssessmentSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsOutcomesAverageGradeService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsOutcomesService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsOutcomesTimeOnTaskService;
import edu.iu.terracotta.utils.TextConstants;

import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAssessmentsByConditionIdAndExposureId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAssessmentsByExposureId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findConsentedParticipantsByGroupId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findExposureGroupConditionByConditionIdAndExposureId;
import static edu.iu.terracotta.service.app.dashboard.results.util.StatisticsUtils.calculateStandardDeviation;
import static edu.iu.terracotta.service.app.dashboard.results.util.StatisticsUtils.calculateStatistics;

@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class ResultsOutcomesServiceImpl implements ResultsOutcomesService {

    @Autowired private AllRepositories allRepositories;
    @Autowired private AssessmentSubmissionService assessmentSubmissionService;
    @Autowired private ResultsOutcomesAverageGradeService resultsOutcomesAverageGradeService;
    @Autowired private ResultsOutcomesTimeOnTaskService resultsOutcomesTimeOnTaskService;
    @Autowired private SubmissionService submissionService;

    private Map<Long, List<Assessment>> allAssessmentsByAssignment;
    private Map<Long, List<Treatment>> allTreatmentsByAssignment;
    private List<Assignment> experimentAssignments;
    private List<Participant> experimentConsentedParticipants;
    private List<ExposureGroupCondition> experimentExposureGroupConditions;
    private List<Exposure> experimentExposures;
    private List<Treatment> experimentTreatments;

    @Override
    public ResultsOutcomesDto outcomes(Experiment experiment, ResultsOutcomesRequestDto resultsOutcomesRequestDto) throws OutcomeNotMatchingException {
        if (!hasOutcomes(resultsOutcomesRequestDto)) {
            throw new IllegalArgumentException(
                String.format(
                    "Exception occurred processing outcomes for experiment ID: [%s]. Request must contain at least one outcome.",
                    experiment.getExperimentId()
                )
            );
        }

        // retrieve all results data for the experiment
        prepareData(experiment);

        if (CollectionUtils.isNotEmpty(resultsOutcomesRequestDto.getOutcomeIds())) {
            // this is a standard outcome calculation
            List<Outcome> outcomes = resultsOutcomesRequestDto.getOutcomeIds().stream()
                .map(
                    outcomeId -> {
                        Optional<Outcome> outcome = allRepositories.outcomeRepository.findById(Long.valueOf(outcomeId));

                        if (outcome.isEmpty()) {
                            // no outcome exists for this ID
                            return null;
                        }

                        if (!experiment.getExperimentId().equals(outcome.get().getExposure().getExperiment().getExperimentId())) {
                            // outcome is not in the experiment
                            return null;
                        }

                        return outcome.get();
                    }
                )
                .filter(outcome -> outcome != null)
                .toList();

            if (resultsOutcomesRequestDto.getOutcomeIds().size() != outcomes.size()) {
                // not all outcomes exist in experiment
                throw new OutcomeNotMatchingException(TextConstants.OUTCOME_NOT_MATCHING);
            }

            return ResultsOutcomesDto.builder()
                .experimentId(experiment.getExperimentId())
                .conditions(conditions(experiment, outcomes))
                .exposures(exposures(outcomes))
                .outcomeType(OutcomeType.STANDARD)
                .build();
        }

        // this is an alternate outcome calculation
        switch (EnumUtils.getEnumIgnoreCase(AlternateIdType.class, resultsOutcomesRequestDto.getAlternateId().getId())) {
            case AVERAGE_ASSIGNMENT_SCORE:
                return ResultsOutcomesDto.builder()
                    .experimentId(experiment.getExperimentId())
                    .conditions(
                        resultsOutcomesAverageGradeService.conditions(
                            experiment,
                            resultsOutcomesRequestDto.getAlternateId().getExposures(),
                            experimentAssignments,
                            allAssessmentsByAssignment,
                            experimentConsentedParticipants,
                            allTreatmentsByAssignment,
                            experimentTreatments
                        )
                    )
                    .exposures(
                        resultsOutcomesAverageGradeService.exposures(
                            resultsOutcomesRequestDto.getAlternateId().getExposures(),
                            experimentAssignments,
                            allAssessmentsByAssignment,
                            experimentConsentedParticipants,
                            experimentExposures
                        )
                    )
                    .outcomeType(OutcomeType.AVERAGE_ASSIGNMENT_SCORE)
                    .build();
            case TIME_ON_TASK:
                return ResultsOutcomesDto.builder()
                    .experimentId(experiment.getExperimentId())
                    .conditions(
                        resultsOutcomesTimeOnTaskService.conditions(
                            experiment,
                            resultsOutcomesRequestDto.getAlternateId().getExposures(),
                            experimentAssignments,
                            allAssessmentsByAssignment,
                            experimentConsentedParticipants,
                            allTreatmentsByAssignment,
                            experimentTreatments
                        )
                    )
                    .exposures(
                        resultsOutcomesTimeOnTaskService.exposures(
                            experiment,
                            resultsOutcomesRequestDto.getAlternateId().getExposures(),
                            experimentAssignments,
                            allAssessmentsByAssignment,
                            experimentConsentedParticipants,
                            experimentExposures
                        )
                    )
                    .outcomeType(OutcomeType.TIME_ON_TASK)
                    .build();
            default:
                return ResultsOutcomesDto.builder()
                    .experimentId(experiment.getExperimentId())
                    .outcomeType(OutcomeType.OTHER)
                    .build();
        }
    }

    /**
     * Create the "Conditions" outcomes data area
     *
     * @param resultsOutcomesRequestDto
     * @return
     */
    private OutcomesConditions conditions(Experiment experiment, List<Outcome> outcomes) {
        return OutcomesConditions.builder()
            .rows(
                experiment.getConditions().stream()
                .map(
                    condition -> {
                        OutcomesConditionBuilder outcomesCondition = OutcomesCondition.builder()
                            .title(condition.getName());
                        List<Double> scores = new ArrayList<>();

                        outcomes.stream()
                            .forEach(
                                outcome -> {
                                    // retrieve exposure group condition
                                    Optional<ExposureGroupCondition> exposureGroupCondition = findExposureGroupConditionByConditionIdAndExposureId(
                                        condition.getConditionId(),
                                        outcome.getExposure().getExposureId(),
                                        experimentExposureGroupConditions
                                    );

                                    if (exposureGroupCondition.isEmpty()) {
                                        return;
                                    }

                                    // get all consented participants in this group
                                    List<Participant> consentedParticipants = findConsentedParticipantsByGroupId(exposureGroupCondition.get().getGroup().getGroupId(), experimentConsentedParticipants);

                                    if (CollectionUtils.isEmpty(consentedParticipants)) {
                                        return;
                                    }

                                    List<Long> consentedParticipantIds = consentedParticipants.stream()
                                        .map(Participant::getParticipantId)
                                        .toList();

                                    // add this group of participant scores to the list
                                    if (BooleanUtils.isNotFalse(outcome.getExternal())) {
                                        // is external outcome; get assignment scores
                                        for (Assessment assessment : findAssessmentsByConditionIdAndExposureId(condition.getConditionId(), outcome.getExposure().getExposureId(), experimentAssignments, allAssessmentsByAssignment)) {
                                            for (Participant participant : experimentConsentedParticipants) {
                                                Float score = submissionService.getScoreFromMultipleSubmissions(participant, assessment);

                                                if (score == null) {
                                                    continue;
                                                }

                                                scores.add(score.doubleValue() / (double) assessmentSubmissionService.calculateMaxScore(assessment));
                                            }
                                        }
                                    } else {
                                        // is internal outcome; get outcome scores
                                        scores.addAll(
                                            outcome.getOutcomeScores().stream()
                                                .filter(outcomeScore -> consentedParticipantIds.contains(outcomeScore.getParticipant().getParticipantId()))
                                                .map(outcomeScore -> (double) outcomeScore.getScoreNumeric() / (double) outcome.getMaxPoints())
                                                .toList()
                                        );
                                    }
                                }
                            );

                        if (CollectionUtils.isEmpty(scores)) {
                            outcomesCondition
                                .mean(0d)
                                .number(0l)
                                .scores(Collections.emptyList())
                                .standardDeviation(0d);
                        } else {
                            DoubleSummaryStatistics outcomesStatistics = calculateStatistics(scores);

                            outcomesCondition
                                .mean(outcomesStatistics.getAverage())
                                .number(outcomesStatistics.getCount())
                                .scores(scores)
                                .standardDeviation(calculateStandardDeviation(scores, outcomesStatistics.getAverage()));
                        }

                        return outcomesCondition.build();
                    }
                )
                .toList()
            )
            .build();
    }

    /**
     * Create the "Exposures" row outcomes data area
     *
     * @param resultsOutcomesRequestDto
     * @return
     */
    private OutcomesExposures exposures(List<Outcome> outcomes) {
        Map<String, List<Double>> outcomesExposuresScores = new HashMap<>();
        List<Long> consentedParticipantIds = experimentConsentedParticipants.stream()
            .map(Participant::getParticipantId)
            .toList();

        // combine outcome scores for each exposure set
        outcomes.forEach(
            outcome -> {
                outcomesExposuresScores.putIfAbsent(outcome.getExposure().getTitle(), new ArrayList<>());

                // retrieve all outcome scores for consenting participants
                if (BooleanUtils.isNotFalse(outcome.getExternal())) {
                    // is external outcome; get assignment scores
                    for (Assessment assessment : findAssessmentsByExposureId(outcome.getExposure().getExposureId(), experimentAssignments, allAssessmentsByAssignment)) {
                        for (Participant participant : experimentConsentedParticipants) {
                            Float score = submissionService.getScoreFromMultipleSubmissions(participant, assessment);

                            if (score == null) {
                                continue;
                            }

                            outcomesExposuresScores.get(outcome.getExposure().getTitle())
                                .add(score.doubleValue() / (double) assessmentSubmissionService.calculateMaxScore(assessment));
                        }
                    }
                } else {
                    // is internal outcome; get outcome scores
                    outcomesExposuresScores.get(outcome.getExposure().getTitle())
                        .addAll(
                            outcome.getOutcomeScores().stream()
                                .filter(outcomeScore -> consentedParticipantIds.contains(outcomeScore.getParticipant().getParticipantId()))
                                .map(outcomeScore -> (double) outcomeScore.getScoreNumeric() / (double) outcome.getMaxPoints())
                                .toList()
                        );
                }
            }
        );

        List<OutcomesExposure> outcomesExposures = outcomesExposuresScores.entrySet().stream()
            .map(
                outcomesExposureScore -> {
                    OutcomesExposureBuilder outcomesExposure = OutcomesExposure.builder()
                        .title(outcomesExposureScore.getKey());

                    if (CollectionUtils.isEmpty(outcomesExposureScore.getValue())) {
                        // no outcome scores exist
                        outcomesExposure
                            .mean(0d)
                            .number(0l)
                            .scores(Collections.emptyList())
                            .standardDeviation(0d);
                    } else {
                        DoubleSummaryStatistics outcomesStatistics = calculateStatistics(outcomesExposureScore.getValue());

                        outcomesExposure
                            .mean(outcomesStatistics.getAverage())
                            .number(outcomesStatistics.getCount())
                            .scores(outcomesExposureScore.getValue())
                            .standardDeviation(calculateStandardDeviation(outcomesExposureScore.getValue(), outcomesStatistics.getAverage()));
                    }

                    return outcomesExposure.build();
                }
            )
            .collect(Collectors.toList()); // need mutable list

        overallExposure(outcomesExposuresScores, outcomesExposures);

        return OutcomesExposures.builder()
            .rows(outcomesExposures)
            .build();
    }

    /**
     * Create the "Overall Exposures" row for outcomes data area
     *
     * @return
     */
    private void overallExposure(Map<String, List<Double>> outcomesExposuresScores, List<OutcomesExposure> outcomesExposures) {
        List<Double> allExposureScores = new ArrayList<>();

        outcomesExposuresScores.entrySet().stream()
            .forEach(
                outcomesExposureScore -> {
                    allExposureScores.addAll(outcomesExposureScore.getValue());
                }
            );

        OutcomesExposureOverallBuilder outcomesExposureOverall = OutcomesExposureOverall.builder()
            .title(OutcomesExposureOverall.EXPOSURE_OVERALL_TITLE);

        if (CollectionUtils.isEmpty(allExposureScores)) {
            // no outcome scores exist
            outcomesExposureOverall
                .mean(0d)
                .number(0l)
                .standardDeviation(0d);
        } else {
            DoubleSummaryStatistics outcomesStatistics = calculateStatistics(allExposureScores);

            outcomesExposureOverall
                .mean(outcomesStatistics.getAverage())
                .number(outcomesStatistics.getCount())
                .standardDeviation(calculateStandardDeviation(allExposureScores, outcomesStatistics.getAverage()));
        }

        outcomesExposures.add(outcomesExposureOverall.build());
    }

    /**
     * Retrieves and prepares necessary data for the results outcomes dashboard
     *
     * @param experiment
     */
    private void prepareData(Experiment experiment) {
        experimentAssignments = allRepositories.assignmentRepository.findByExposure_Experiment_ExperimentId(experiment.getExperimentId());

        allAssessmentsByAssignment = new HashMap<>();

        for (Assignment experimentAssignment : experimentAssignments) {
            allAssessmentsByAssignment.put(
                experimentAssignment.getAssignmentId(),
                allRepositories.assessmentRepository.findByTreatment_Assignment_AssignmentId(experimentAssignment.getAssignmentId())
            );
        }

        experimentConsentedParticipants = allRepositories.participantRepository.findByExperiment_ExperimentId(experiment.getExperimentId()).stream()
            .filter(participant -> !participant.isTestStudent())
            .filter(participant -> BooleanUtils.isTrue(participant.getConsent()))
            .toList();

        experimentExposureGroupConditions = allRepositories.exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experiment.getExperimentId());
        experimentExposures = allRepositories.exposureRepository.findByExperiment_ExperimentId(experiment.getExperimentId());

        experimentTreatments = allRepositories.treatmentRepository.findByCondition_Experiment_ExperimentId(experiment.getExperimentId());

        allTreatmentsByAssignment = new HashMap<>();

        for (Assignment assignment : experimentAssignments) {
            allTreatmentsByAssignment.put(
                assignment.getAssignmentId(),
                allRepositories.treatmentRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
            );
        }
    }

    private boolean hasOutcomes(ResultsOutcomesRequestDto resultsOutcomesRequestDto) {
        if (CollectionUtils.isNotEmpty(resultsOutcomesRequestDto.getOutcomeIds())) {
            return true;
        }

        return StringUtils.isNotBlank(resultsOutcomesRequestDto.getAlternateId().getId()) &&
            CollectionUtils.isNotEmpty(resultsOutcomesRequestDto.getAlternateId().getExposures());
    }

}
