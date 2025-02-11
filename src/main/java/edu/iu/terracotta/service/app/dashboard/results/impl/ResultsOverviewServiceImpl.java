package edu.iu.terracotta.service.app.dashboard.results.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.ResultsOverviewDto;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.OverviewAssignment;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.OverviewAssignment.OverviewAssignmentBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.OverviewAssignments;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.treatment.OverviewTreatment;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.treatment.OverviewTreatment.OverviewTreatmentBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.treatment.OverviewTreatments;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.condition.OverviewCondition;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.condition.OverviewCondition.OverviewConditionBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.condition.OverviewConditionSingle;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.condition.OverviewConditions;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.condition.OverviewConditions.OverviewConditionsBuilder;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.grade.Grade;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.participant.OverviewParticipant;
import edu.iu.terracotta.dao.model.dto.dashboard.results.overview.participant.OverviewParticipant.OverviewParticipantBuilder;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.AssessmentSubmissionService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.SubmissionService;
import edu.iu.terracotta.service.app.dashboard.results.ResultsOverviewService;
import edu.iu.terracotta.service.app.dashboard.results.util.StatisticsUtils;
import lombok.extern.slf4j.Slf4j;

import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.countTreatmentsByAssignmentId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAllLmsAssignmentIds;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAssessmentsByConditionId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findAssessmentsByTreatmentId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findLmsAssignmentByLmsAssignmentId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findExposureGroupsByConditionId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findParticipantsByGroupId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findSubmissionsByAssignmentId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findSubmissionsByTreatmentId;
import static edu.iu.terracotta.service.app.dashboard.results.util.ListDataUtils.findTreatmentsByAssignmentId;
import static edu.iu.terracotta.service.app.dashboard.results.util.StatisticsUtils.calculateStatistics;
import static edu.iu.terracotta.dao.model.dto.dashboard.results.overview.assignment.OverviewAssignmentOverall.ASSIGNMENT_OVERALL_TITLE;

@Slf4j
@Service
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class ResultsOverviewServiceImpl implements ResultsOverviewService {

    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AssessmentSubmissionService assessmentSubmissionService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private SubmissionService submissionService;

    private Map<Long, List<Assessment>> allAssessmentsByAssignment;
    private Map<Long, List<Treatment>> allTreatmentsByAssignment;
    private List<Assignment> experimentAssignments;
    private List<LmsAssignment> experimentLmsAssignments;
    private List<Participant> experimentConsentedParticipants;
    private List<ExposureGroupCondition> experimentExposureGroupConditions;
    private List<Participant> experimentParticipants;
    private List<Submission> experimentSubmissions;
    private List<Treatment> experimentTreatments;

    @Override
    public ResultsOverviewDto overview(Experiment experiment, SecuredInfo securedInfo) {
        // retrieve all results data for the experiment
        prepareData(experiment, securedInfo);

        return ResultsOverviewDto.builder()
            .assignments(assignments())
            .conditions(conditions(experiment))
            .participants(participants())
            .build();
    }

    /**
     * Create the "Assignments" overview data area
     *
     * @return
     */
    private OverviewAssignments assignments() {
        if (CollectionUtils.isEmpty(experimentAssignments)) {
            return OverviewAssignments.builder().build();
        }

        // create assignment summaries
        List<OverviewAssignment> overviewAssignments = experimentAssignments.stream()
            .map(
                experimentAssignment -> {
                    OverviewAssignmentBuilder overviewAssignment = OverviewAssignment.builder()
                        .title(experimentAssignment.getTitle());

                    int submissionCount = findSubmissionsByAssignmentId(experimentAssignment.getAssignmentId(), experimentSubmissions).size();

                    Grade grade = grades(allAssessmentsByAssignment.get(experimentAssignment.getAssignmentId()));
                    overviewAssignment
                        .open(calculateOpenAssignment(experimentAssignment))
                        .id(experimentAssignment.getAssignmentId())
                        .submissionCount(submissionCount)
                        .averageGrade(grade.getAverage())
                        .standardDeviation(grade.getStandardDeviation());

                    if (CollectionUtils.isEmpty(experimentConsentedParticipants)) {
                        overviewAssignment.submissionRate(0f);
                    } else {
                        overviewAssignment.submissionRate((double) submissionCount / (double) experimentConsentedParticipants.size());
                    }

                    overviewAssignment.treatments(treatments(experimentAssignment));

                    return overviewAssignment.build();
                }
            )
            .collect(Collectors.toList()); // need mutable list

        // create assignment overall overview
        overallAssignment(overviewAssignments);

        return OverviewAssignments.builder()
            .rows(overviewAssignments)
            .build();
    }

    private boolean calculateOpenAssignment(Assignment experimentAssignment) {
        Optional<LmsAssignment> lmsAssignment = findLmsAssignmentByLmsAssignmentId(experimentAssignment.getLmsAssignmentId(), experimentLmsAssignments);

        if (lmsAssignment.isEmpty()) {
            return true;
        }

        Date now = new Date();

        if (lmsAssignment.get().getUnlockAt() == null && lmsAssignment.get().getLockAt() == null) {
            // both unlock and lock are null; assignment open
            return true;
        }

        if (lmsAssignment.get().getUnlockAt() != null && lmsAssignment.get().getLockAt() == null) {
            // unlock set and lock is null; check now is after unlock
            return now.after(lmsAssignment.get().getUnlockAt());
        }

        if (lmsAssignment.get().getUnlockAt() == null && lmsAssignment.get().getLockAt() != null) {
            // lock set and unlock is null; check now is before lock
            return now.before(lmsAssignment.get().getLockAt());
        }

        return lmsAssignment.get().getUnlockAt() != null && lmsAssignment.get().getLockAt() != null &&
            now.after(lmsAssignment.get().getUnlockAt()) && now.before(lmsAssignment.get().getLockAt());
    }

    /**
     * Create the "Assignments" Treatment overview data area
     *
     * @return
     */
    private OverviewTreatments treatments(Assignment assignment) {
        return OverviewTreatments.builder()
            .rows(
                CollectionUtils.emptyIfNull(findTreatmentsByAssignmentId(assignment.getAssignmentId(), experimentTreatments)).stream()
                    .map(treatment -> {
                        OverviewTreatmentBuilder overviewTreatment = OverviewTreatment.builder();

                        int submissionCount = findSubmissionsByTreatmentId(treatment.getTreatmentId(), experimentSubmissions).size();

                        Grade grade = grades(findAssessmentsByTreatmentId(treatment.getTreatmentId(), allAssessmentsByAssignment, experimentTreatments));
                        overviewTreatment
                            .assignmentId(assignment.getAssignmentId())
                            .conditionId(treatment.getCondition().getConditionId())
                            .averageGrade(grade.getAverage())
                            .id(treatment.getTreatmentId())
                            .standardDeviation(grade.getStandardDeviation())
                            .submissionCount(submissionCount);

                        if (CollectionUtils.isEmpty(experimentConsentedParticipants)) {
                            overviewTreatment.submissionRate(0f);
                        } else {
                            overviewTreatment.submissionRate((double) submissionCount / (double) experimentConsentedParticipants.size());
                        }

                        return overviewTreatment.build();
                    })
                    .toList()
            )
            .build();
    }

    /**
     * Calculate the "Overall" assignment overview row
     *
     * @param overviewAssignments
     */
    private void overallAssignment(List<OverviewAssignment> overviewAssignments) {
        OverviewAssignmentBuilder overviewAssignmentOverall = OverviewAssignment.builder()
            .title(ASSIGNMENT_OVERALL_TITLE);
        AtomicLong submissionCount = new AtomicLong(0l);

        overviewAssignments.forEach(
            assignmentOverview -> submissionCount.addAndGet(assignmentOverview.getSubmissionCount())
        );

        overviewAssignmentOverall.submissionCount(submissionCount.get());

        if (CollectionUtils.isEmpty(experimentConsentedParticipants)) {
            overviewAssignmentOverall.submissionRate(0f);
        } else {
            overviewAssignmentOverall.submissionRate((double) submissionCount.get() / (double) experimentConsentedParticipants.size());
        }

        // single list of all assessments
        List<Assessment> experimentAssessments = new ArrayList<>();

        allAssessmentsByAssignment.values().stream()
            .forEach(
                assignmentAssessments -> {
                    assignmentAssessments.forEach(
                        assignmentAssessment -> {
                            experimentAssessments.add(assignmentAssessment);
                        }
                    );
                }
            );

        Grade grade = grades(experimentAssessments);
        overviewAssignmentOverall
            .averageGrade(grade.getAverage())
            .standardDeviation(grade.getStandardDeviation());
        overviewAssignments.add(overviewAssignmentOverall.build());
    }

    /**
     * Calculate the "Average Grades" overview column
     *
     * @param assessments
     * @return
     */
    private Grade grades(List<Assessment> assessments) {
        List<Double> assignmentScores = new ArrayList<>();
        AtomicBoolean hasOnlyMC = new AtomicBoolean(true);

        assessments.forEach(
            assessment -> {
                hasOnlyMC.set(
                    hasOnlyMC.get() &&
                    assessment.getQuestions().stream()
                        .filter(question -> question.getQuestionType() != QuestionTypes.MC)
                        .filter(question -> question.getQuestionType() != QuestionTypes.PAGE_BREAK)
                        .count() == 0
                );

                experimentConsentedParticipants.forEach(
                    ecp -> {
                        Float assignmentScore = submissionService.getScoreFromMultipleSubmissions(ecp, assessment);

                        if (assignmentScore == null) {
                            return;
                        }

                        assignmentScores.add(assignmentScore.doubleValue() / assessmentSubmissionService.calculateMaxScore(assessment));
                    }
                );
            }
        );

        DoubleSummaryStatistics gradeOverviewStatistics = calculateStatistics(assignmentScores);
        double average = gradeOverviewStatistics.getAverage();

        if (CollectionUtils.isEmpty(assignmentScores) && !hasOnlyMC.get()) {
            // send negative to notify front end no graded assignments exist
            average = -1d;
        }

        return Grade.builder()
            .average(average)
            .standardDeviation(StatisticsUtils.calculateStandardDeviation(assignmentScores, gradeOverviewStatistics.getAverage()))
            .build();
    }

    /**
     * Create the "Conditons" overview data area
     *
     * @param experiment
     * @return
     */
    private OverviewConditions conditions(Experiment experiment) {
        OverviewConditionsBuilder overviewConditions = OverviewConditions.builder();

        if (CollectionUtils.isEmpty(experiment.getConditions())) {
            return overviewConditions.build();
        }

        List<OverviewCondition> overviewConditionList = experiment.getConditions().stream()
            .map(
                experimentCondition -> {
                    List<Assessment> assessments = findAssessmentsByConditionId(experimentCondition.getConditionId(), experimentTreatments);

                    if (experiment.getConditions().size() > 1) {
                        // filter out single-condition assignment assessments
                        assessments = assessments.stream()
                            .filter(a -> countTreatmentsByAssignmentId(a.getTreatment().getAssignment().getAssignmentId(), allTreatmentsByAssignment) > 1)
                        .toList();
                    }

                    Grade grade = grades(assessments);
                    OverviewConditionBuilder overviewCondition = OverviewCondition.builder()
                        .title(experimentCondition.getName())
                        .averageGrade(grade.getAverage())
                        .standardDeviation(grade.getStandardDeviation());

                    AtomicLong submissionsCount = new AtomicLong(0);

                    assessments
                        .forEach(
                            assessment -> {
                                submissionsCount.addAndGet(
                                    // filter out non-consenting participant submissions
                                    submissionRepository.findByAssessment_AssessmentId(assessment.getAssessmentId()).stream()
                                        .filter(submission -> BooleanUtils.isTrue(submission.getParticipant().getConsent()))
                                        .filter(submission -> submission.getParticipant().getDateRevoked() == null)
                                        .count()
                                );
                            }
                        );

                    overviewCondition.submissionCount(submissionsCount.get());

                    if (submissionsCount.get() == 0) {
                        overviewCondition.submissionRate(0);
                    } else {
                        AtomicLong particpantCount = new AtomicLong(0);

                        findExposureGroupsByConditionId(experimentCondition.getConditionId(), experimentExposureGroupConditions)
                            .forEach(
                                egc -> {
                                    particpantCount.addAndGet(findParticipantsByGroupId(experimentConsentedParticipants, egc.getGroup().getGroupId()).size());
                                });

                        if (particpantCount.get() == 0) {
                            overviewCondition.submissionRate(0);
                        } else {
                            overviewCondition.submissionRate((double) submissionsCount.get() / (double) particpantCount.get());
                        }
                    }

                    return overviewCondition.build();
                }
            )
            .collect(Collectors.toList()); // need mutable list

        if (experiment.getConditions().size() > 1) {
            singleCondition(experiment, overviewConditionList);
        }

        overviewConditions
            .exposureType(experiment.getExposureType())
            .rows(overviewConditionList);

        return overviewConditions.build();
    }

    /**
     * Create the "Conditons" overview "Single Condition Assignment" data row area
     *
     * @param experiment
     * @param overviewConditionList
     * @return
     */
    private void singleCondition(Experiment experiment, List<OverviewCondition> overviewConditionList) {
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

        Grade grade = grades(assessments);
        OverviewConditionBuilder overviewCondition = OverviewCondition.builder()
            .title(OverviewConditionSingle.CONDITION_SINGLE_TITLE)
            .averageGrade(grade.getAverage())
            .standardDeviation(grade.getStandardDeviation());

        AtomicLong submissionsCount = new AtomicLong(0);

        assessments
            .forEach(
                assessment -> {
                    submissionsCount.addAndGet(submissionRepository.countByAssessment_AssessmentId(assessment.getAssessmentId()));
                }
            );

        overviewCondition.submissionCount(submissionsCount.get());

        if (submissionsCount.get() == 0) {
            overviewCondition.submissionRate(0);
        } else {
            if (CollectionUtils.isEmpty(experimentConsentedParticipants)) {
                overviewCondition.submissionRate(0);
            } else {
                overviewCondition.submissionRate((double) submissionsCount.get() / (double) experimentConsentedParticipants.size());
            }
        }

        overviewConditionList.add(overviewCondition.build());
    }

    /**
     * Create the "Participants" overview data area
     *
     * @param experiment
     * @return
     */
    private OverviewParticipant participants() {
        OverviewParticipantBuilder overviewParticipant = OverviewParticipant.builder();

        if (CollectionUtils.isEmpty(experimentParticipants)) {
            return overviewParticipant.build();
        }

        overviewParticipant
            .count(experimentConsentedParticipants.size())
            .classEnrollment(experimentParticipants.size())
            .consentRate((double) experimentConsentedParticipants.size() / (double) experimentParticipants.size())
            .assignmentCount(experimentAssignments.size());

        return overviewParticipant.build();
    }

    /**
     * Retrieves and prepares necessary data for the results overview dashboard
     *
     * @param experiment
     */
    private void prepareData(Experiment experiment, SecuredInfo securedInfo) {
        experimentAssignments = assignmentRepository.findByExposure_Experiment_ExperimentId(experiment.getExperimentId());

        experimentParticipants = participantRepository.findByExperiment_ExperimentId(experiment.getExperimentId()).stream()
            .filter(participant -> !participant.isTestStudent())
            .toList();

        experimentConsentedParticipants = CollectionUtils.emptyIfNull(experimentParticipants).stream()
            .filter(participant -> BooleanUtils.isTrue(participant.getConsent()))
            .toList();

        allAssessmentsByAssignment = new HashMap<>();

        for (Assignment experimentAssignment : experimentAssignments) {
            allAssessmentsByAssignment.put(
                experimentAssignment.getAssignmentId(),
                assessmentRepository.findByTreatment_Assignment_AssignmentId(experimentAssignment.getAssignmentId())
            );
        }

        experimentTreatments = treatmentRepository.findByCondition_Experiment_ExperimentId(experiment.getExperimentId());

        allTreatmentsByAssignment = new HashMap<>();

        for (Assignment assignment : experimentAssignments) {
            allTreatmentsByAssignment.put(
                assignment.getAssignmentId(),
                treatmentRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())
            );
        }

        experimentExposureGroupConditions = exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experiment.getExperimentId());
        // experiment submissions without non-consented participants' submissions
        experimentSubmissions = submissionRepository.findByParticipant_Experiment_ExperimentId(experiment.getExperimentId()).stream()
            .filter(submission -> BooleanUtils.isTrue(submission.getParticipant().getConsent()))
            .filter(submission -> submission.getParticipant().getDateRevoked() == null)
            .toList();

        experimentLmsAssignments = findAllLmsAssignmentIds(experimentAssignments).stream()
            .map(
                lmsAssignmentId -> {
                    Optional<LmsAssignment> lmsAssignment;

                    try {
                        lmsAssignment = assignmentService.getLmsAssignmentById(lmsAssignmentId, securedInfo);
                    } catch (ApiException | TerracottaConnectorException e) {
                        log.error("Error retrieving assignments from LMS for course ID: [{}]", securedInfo.getLmsCourseId(), e);
                        return null;
                    }

                    if (lmsAssignment.isEmpty()) {
                        return null;
                    }

                    return lmsAssignment.get();
                }
            )
            .filter(assignmentExtended -> !Objects.isNull(assignmentExtended))
            .toList();
    }

}
