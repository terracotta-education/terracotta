package edu.iu.terracotta.service.app.dashboard.results.impl;

import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.countTreatmentsByAssignmentId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAssessmentsByConditionId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAssessmentsByConditionIdAndExposureIds;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAssessmentsByExposureId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findExposureTitleByExposureId;
import static edu.iu.terracotta.service.app.dashboard.results.util.StatisticsUtils.calculateStandardDeviation;
import static edu.iu.terracotta.service.app.dashboard.results.util.StatisticsUtils.calculateStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesCondition;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesCondition.OutcomesConditionBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesConditionSingle;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesConditionSingle.OutcomesConditionSingleBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.condition.OutcomesConditions;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposure;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposure.OutcomesExposureBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposureOverall;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposureOverall.OutcomesExposureOverallBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.outcomes.exposure.OutcomesExposures;
import edu.iu.terracotta.service.app.AssessmentSubmissionService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsOutcomesAverageGradeService;

@Service
@SuppressWarnings({"rawtypes", "unchecked"})
public class ResultsOutcomesAverageGradeServiceImpl implements ResultsOutcomesAverageGradeService {

    @Autowired private AssessmentSubmissionService assessmentSubmissionService;
    @Autowired private SubmissionService submissionService;

    @Override
    public OutcomesConditions conditions(Experiment experiment, List<Long> exposureIds, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment, List<Participant> experimentConsentedParticipants, Map<Long, List<Treatment>> allTreatmentsByAssignment, List<Treatment> experimentTreatments) {
        List<OutcomesCondition> outcomesConditions = experiment.getConditions().stream()
            .map(
                condition -> {
                    OutcomesConditionBuilder outcomesCondition = OutcomesCondition.builder()
                        .title(condition.getName());
                    List<Assessment> assessments = findAssessmentsByConditionIdAndExposureIds(condition.getConditionId(), exposureIds, experimentAssignments, allAssessmentsByAssignment);

                    if (experiment.getConditions().size() > 1) {
                        // filter out single-condition assignment assessments
                        assessments = assessments.stream()
                            .filter(a -> countTreatmentsByAssignmentId(a.getTreatment().getAssignment().getAssignmentId(), allTreatmentsByAssignment) > 1)
                            .toList();
                    }

                    conditionScores(outcomesCondition, assessments, experimentConsentedParticipants);

                    return outcomesCondition.build();
                }
            )
            .collect(Collectors.toList()); // need mutable list

        if (experiment.getConditions().size() > 1) {
            singleCondition(experiment, outcomesConditions, experimentTreatments, allTreatmentsByAssignment, experimentConsentedParticipants);
        }

        return OutcomesConditions.builder()
            .rows(outcomesConditions)
            .build();
    }

    private void singleCondition(Experiment experiment, List<OutcomesCondition> outcomesConditions, List<Treatment> experimentTreatments, Map<Long, List<Treatment>> allTreatmentsByAssignment, List<Participant> experimentConsentedParticipants) {
        if (CollectionUtils.isEmpty(experiment.getConditions())) {
            return;
        }

        List<Assessment> assessments = new ArrayList<>();

        // filter out non-single-condition assignment assessments
        experiment.getConditions()
            .forEach(
                experimentCondition -> {
                    assessments.addAll(findAssessmentsByConditionId(experimentCondition.getConditionId(), experimentTreatments).stream()
                        .filter(a -> countTreatmentsByAssignmentId(a.getTreatment().getAssignment().getAssignmentId(), allTreatmentsByAssignment) == 1)
                        .toList()
                    );
                }
            );

        if (CollectionUtils.isEmpty(assessments)) {
            return;
        }

        OutcomesConditionSingleBuilder outcomesConditionSingle = OutcomesConditionSingle.builder()
            .title(OutcomesConditionSingle.CONDITION_SINGLE_TITLE);

        conditionScores(outcomesConditionSingle, assessments, experimentConsentedParticipants);

        outcomesConditions.add(outcomesConditionSingle.build());
    }

    private void conditionScores(OutcomesConditionBuilder outcomesCondition, List<Assessment> assessments, List<Participant> experimentConsentedParticipants) {
        List<Double> scores = new ArrayList<>();

        for (Assessment assessment : assessments) {
            for (Participant participant : experimentConsentedParticipants) {
                Float score = submissionService.getScoreFromMultipleSubmissions(participant, assessment);

                if (score == null) {
                    continue;
                }

                scores.add(score.doubleValue() / (double) assessmentSubmissionService.calculateMaxScore(assessment));
            }
        }

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
    }

    @Override
    public OutcomesExposures exposures(List<Long> exposureIds, List<Assignment> experimentAssignments, Map<Long, List<Assessment>> allAssessmentsByAssignment, List<Participant> experimentConsentedParticipants, List<Exposure> experimentExposures) {
        Map<String, List<Double>> exposuresScores = new HashMap<>();

        exposureIds.forEach(
            exposureId -> {
                List<Double> scores = new ArrayList<>();

                for (Participant participant : experimentConsentedParticipants) {
                    for (Assessment assessment : findAssessmentsByExposureId(exposureId, experimentAssignments, allAssessmentsByAssignment)) {
                        Float score = submissionService.getScoreFromMultipleSubmissions(participant, assessment);

                        if (score == null) {
                            continue;
                        }

                        scores.add(score.doubleValue() / (double) assessmentSubmissionService.calculateMaxScore(assessment));
                    }
                }

                exposuresScores.putIfAbsent(findExposureTitleByExposureId(exposureId, experimentExposures), new ArrayList<>());
                exposuresScores.get(findExposureTitleByExposureId(exposureId, experimentExposures)).addAll(scores);
            }
        );

        List<OutcomesExposure> outcomesExposures = exposuresScores.entrySet().stream()
            .map(
                exposureScore -> {
                    OutcomesExposureBuilder outcomesExposure = OutcomesExposure.builder()
                        .title(exposureScore.getKey());

                    if (CollectionUtils.isEmpty(exposureScore.getValue())) {
                        // no outcome scores exist
                        outcomesExposure
                            .mean(0d)
                            .number(0l)
                            .scores(Collections.emptyList())
                            .standardDeviation(0d);
                    } else {
                        DoubleSummaryStatistics outcomesStatistics = calculateStatistics(exposureScore.getValue());

                        outcomesExposure
                            .mean(outcomesStatistics.getAverage())
                            .number(outcomesStatistics.getCount())
                            .scores(exposureScore.getValue())
                            .standardDeviation(calculateStandardDeviation(exposureScore.getValue(), outcomesStatistics.getAverage()));
                    }

                    return outcomesExposure.build();
                }
            )
            .collect(Collectors.toList()); // need mutable list

        overallExposure(exposuresScores, outcomesExposures);

        return OutcomesExposures.builder()
            .rows(outcomesExposures)
            .build();
    }

    /**
     * Calculate the Overall Exposure row.
     *
     * @param exposuresScores
     * @param outcomesExposures
     */
    private void overallExposure(Map<String, List<Double>> exposuresScores, List<OutcomesExposure> outcomesExposures) {
        List<Double> allExposureScores = new ArrayList<>();

        exposuresScores.entrySet().stream()
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

}
