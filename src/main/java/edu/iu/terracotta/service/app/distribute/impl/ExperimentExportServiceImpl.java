package edu.iu.terracotta.service.app.distribute.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.model.distribute.export.AnswerMcExport;
import edu.iu.terracotta.dao.model.distribute.export.AssessmentExport;
import edu.iu.terracotta.dao.model.distribute.export.AssignmentExport;
import edu.iu.terracotta.dao.model.distribute.export.ConditionExport;
import edu.iu.terracotta.dao.model.distribute.export.ConsentDocumentExport;
import edu.iu.terracotta.dao.model.distribute.export.ExperimentExport;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.distribute.export.ExposureExport;
import edu.iu.terracotta.dao.model.distribute.export.ExposureGroupConditionExport;
import edu.iu.terracotta.dao.model.distribute.export.GroupExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationClientExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationConfigurationExport;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationExport;
import edu.iu.terracotta.dao.model.distribute.export.OriginExport;
import edu.iu.terracotta.dao.model.distribute.export.OutcomeExport;
import edu.iu.terracotta.dao.model.distribute.export.QuestionExport;
import edu.iu.terracotta.dao.model.distribute.export.TreatmentExport;
import edu.iu.terracotta.dao.model.dto.distribute.ExportDto;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.repository.AnswerMcRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.OutcomeRepository;
import edu.iu.terracotta.dao.repository.QuestionMcRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.ExperimentExportException;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.distribute.ExperimentExportService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentExportServiceImpl implements ExperimentExportService {

    @Autowired private AnswerMcRepository answerMcRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private OutcomeRepository outcomeRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionMcRepository questionMcRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private FileStorageService fileStorageService;

    private Experiment experiment;
    private List<AnswerMc> answersMc;
    private List<Assessment> assessments;
    private List<Assignment> assignments;
    private List<ExposureGroupCondition> exposureGroupConditions;
    private List<Group> groups;
    private List<Integration> integrations;
    private List<Outcome> outcomes;
    private List<Question> questions;
    private List<Treatment> treatments;

    @Override
    public ExportDto export(Experiment experiment) throws ExperimentExportException {
        prepareData(experiment);

        Export export = Export.builder()
            .answersMc(answerMcExport())
            .assessments(assessmentExport())
            .assignments(assignmentExport())
            .conditions(conditionExport())
            .consentDocument(consentDocumentExport())
            .experiment(experimentExport())
            .exposureGroupConditions(exposureGroupConditionExport())
            .exposures(exposureExport())
            .groups(groupExport())
            .integrationClients(integrationClientExport())
            .integrationConfigurations(integrationConfigurationExport())
            .integrations(integrationExport())
            .origin(originExport())
            .outcomes(outcomeExport())
            .questions(questionExport())
            .treatments(treatmentExport())
            .build();

        return createExportFile(export);
    }

    private List<AnswerMcExport> answerMcExport() {
        return CollectionUtils.emptyIfNull(answersMc).stream()
            .map(
                answerMc ->
                    AnswerMcExport.builder()
                        .answerOrder(answerMc.getAnswerOrder())
                        .correct(answerMc.getCorrect())
                        .html(answerMc.getHtml())
                        .id(answerMc.getAnswerMcId())
                        .questionId(answerMc.getQuestion().getQuestionId())
                        .build()
            )
            .toList();
    }

    private List<AssessmentExport> assessmentExport() {
        return CollectionUtils.emptyIfNull(assessments).stream()
            .map(
                assessment ->
                    AssessmentExport.builder()
                        .allowStudentViewCorrectAnswers(assessment.isAllowStudentViewCorrectAnswers())
                        .allowStudentViewResponses(assessment.isAllowStudentViewResponses())
                        .autoSubmit(assessment.isAutoSubmit())
                        .cumulativeScoringInitialPercentage(assessment.getCumulativeScoringInitialPercentage())
                        .hoursBetweenSubmissions(assessment.getHoursBetweenSubmissions())
                        .html(assessment.getHtml())
                        .id(assessment.getAssessmentId())
                        .multipleSubmissionScoringScheme(assessment.getMultipleSubmissionScoringScheme())
                        .numOfSubmissions(assessment.getNumOfSubmissions())
                        .studentViewCorrectAnswersAfter(assessment.getStudentViewCorrectAnswersAfter())
                        .studentViewCorrectAnswersBefore(assessment.getStudentViewCorrectAnswersBefore())
                        .studentViewResponsesAfter(assessment.getStudentViewResponsesAfter())
                        .studentViewResponsesBefore(assessment.getStudentViewResponsesBefore())
                        .title(assessment.getTitle())
                        .treatmentId(assessment.getTreatment().getTreatmentId())
                        .build()
            )
            .toList();
    }

    private List<AssignmentExport> assignmentExport() {
        return CollectionUtils.emptyIfNull(assignments).stream()
            .map(
                assignment ->
                    AssignmentExport.builder()
                        .allowStudentViewCorrectAnswers(assignment.isAllowStudentViewCorrectAnswers())
                        .allowStudentViewResponses(assignment.isAllowStudentViewResponses())
                        .assignmentOrder(assignment.getAssignmentOrder())
                        .cumulativeScoringInitialPercentage(assignment.getCumulativeScoringInitialPercentage())
                        .exposureId(assignment.getExposure().getExposureId())
                        .hoursBetweenSubmissions(assignment.getHoursBetweenSubmissions())
                        .id(assignment.getAssignmentId())
                        .multipleSubmissionScoringScheme(assignment.getMultipleSubmissionScoringScheme())
                        .numOfSubmissions(assignment.getNumOfSubmissions())
                        .studentViewCorrectAnswersAfter(assignment.getStudentViewCorrectAnswersAfter())
                        .studentViewCorrectAnswersBefore(assignment.getStudentViewCorrectAnswersBefore())
                        .studentViewResponsesAfter(assignment.getStudentViewResponsesAfter())
                        .studentViewResponsesBefore(assignment.getStudentViewResponsesBefore())
                        .title(assignment.getTitle())
                        .build()
            )
            .toList();
    }

    private List<ConditionExport> conditionExport() {
        return CollectionUtils.emptyIfNull(experiment.getConditions()).stream()
            .map(
                condition ->
                    ConditionExport.builder()
                        .defaultCondition(condition.getDefaultCondition())
                        .distributionPct(condition.getDistributionPct())
                        .experimentId(condition.getExperiment().getExperimentId())
                        .id(condition.getConditionId())
                        .name(condition.getName())
                        .build()
            )
            .toList();
    }

    private ConsentDocumentExport consentDocumentExport() {
        if (ParticipationTypes.CONSENT != experiment.getParticipationType()) {
            // experiment is not consent; exit
            return null;
        }

        return ConsentDocumentExport.builder()
            .experimentId(experiment.getExperimentId())
            .html(experiment.getConsentDocument().getHtml())
            .id(experiment.getConsentDocument().getConsentDocumentId())
            .title(experiment.getConsentDocument().getTitle())
            .build();
    }

    private ExperimentExport experimentExport() {
        return ExperimentExport.builder()
            .description(experiment.getDescription())
            .distributionType(experiment.getDistributionType())
            .exposureType(experiment.getExposureType())
            .id(experiment.getExperimentId())
            .participationType(experiment.getParticipationType())
            .title(experiment.getTitle())
            .build();
    }

    private List<ExposureExport> exposureExport() {
        return CollectionUtils.emptyIfNull(experiment.getExposures()).stream()
            .map(
                exposure ->
                    ExposureExport.builder()
                        .experimentId(experiment.getExperimentId())
                        .id(exposure.getExposureId())
                        .title(exposure.getTitle())
                        .build()
            )
            .toList();
    }

    private List<ExposureGroupConditionExport> exposureGroupConditionExport() {
        return CollectionUtils.emptyIfNull(exposureGroupConditions).stream()
            .map(
                exposureGroupCondition ->
                    ExposureGroupConditionExport.builder()
                        .conditionId(exposureGroupCondition.getCondition().getConditionId())
                        .exposureId(exposureGroupCondition.getExposure().getExposureId())
                        .groupId(exposureGroupCondition.getGroup().getGroupId())
                        .id(exposureGroupCondition.getExposureGroupConditionId())
                        .build()
            )
            .toList();
    }

    private List<GroupExport> groupExport() {
        return CollectionUtils.emptyIfNull(groups).stream()
            .map(
                group ->
                    GroupExport.builder()
                        .experimentId(group.getExperiment().getExperimentId())
                        .id(group.getGroupId())
                        .name(group.getName())
                        .build()
            )
            .toList();
    }

    private List<IntegrationClientExport> integrationClientExport() {
        return CollectionUtils.emptyIfNull(integrations).stream()
            .map(
                integration ->
                    IntegrationClientExport.builder()
                        .enabled(integration.getConfiguration().getClient().isEnabled())
                        .id(integration.getConfiguration().getClient().getId())
                        .name(integration.getConfiguration().getClient().getName())
                        .previewToken(integration.getConfiguration().getClient().getPreviewToken())
                        .scoreVariable(integration.getConfiguration().getClient().getScoreVariable())
                        .tokenVariable(integration.getConfiguration().getClient().getTokenVariable())
                        .build()
            )
            .toList();
    }

    private List<IntegrationConfigurationExport> integrationConfigurationExport() {
        return CollectionUtils.emptyIfNull(integrations).stream()
            .map(
                integration ->
                    IntegrationConfigurationExport.builder()
                        .clientId(integration.getConfiguration().getClient().getId())
                        .id(integration.getConfiguration().getId())
                        .integrationId(integration.getId())
                        .launchUrl(integration.getConfiguration().getLaunchUrl())
                        .build()
            )
            .toList();
    }

    private List<IntegrationExport> integrationExport() {
        return CollectionUtils.emptyIfNull(integrations).stream()
            .map(
                integration ->
                    IntegrationExport.builder()
                        .configurationId(integration.getConfiguration().getId())
                        .id(integration.getId())
                        .questionId(integration.getQuestion().getQuestionId())
                        .build()
            )
            .toList();
    }

    private List<OutcomeExport> outcomeExport() {
        return CollectionUtils.emptyIfNull(outcomes).stream()
            .map(
                outcome ->
                    OutcomeExport.builder()
                        .exposureId(outcome.getExposure().getExposureId())
                        .id(outcome.getOutcomeId())
                        .maxPoints(outcome.getMaxPoints())
                        .title(outcome.getTitle())
                        .build()
            )
            .toList();
    }

    private List<QuestionExport> questionExport() {
        return CollectionUtils.emptyIfNull(questions).stream()
            .map(
                question -> {
                    boolean randomizeAnswers = false;

                    if (question.isMC()) {
                        randomizeAnswers = questionMcRepository.findByQuestionId(question.getQuestionId()).get().isRandomizeAnswers();
                    }

                    return QuestionExport.builder()
                        .assessmentId(question.getAssessment().getAssessmentId())
                        .html(question.getHtml())
                        .id(question.getQuestionId())
                        .integrationId(question.getIntegration() != null ? question.getIntegration().getId() : null)
                        .points(question.getPoints())
                        .questionOrder(question.getQuestionOrder())
                        .questionType(question.getQuestionType())
                        .randomizeAnswers(randomizeAnswers)
                        .regradeOption(question.getRegradeOption())
                        .build();
                }
            )
            .toList();
    }

    private List<TreatmentExport> treatmentExport() {
        return CollectionUtils.emptyIfNull(treatments).stream()
            .map(
                treatment ->
                    TreatmentExport.builder()
                        .assessmentId(treatment.getAssessment().getAssessmentId())
                        .assignmentId(treatment.getAssignment().getAssignmentId())
                        .conditionId(treatment.getCondition().getConditionId())
                        .id(treatment.getTreatmentId())
                        .build()
            )
            .toList();
    }

    private OriginExport originExport() {
        return OriginExport.builder()
            .institutionUrl(experiment.getPlatformDeployment().getBaseUrl())
            .courseTitle(experiment.getLtiContextEntity().getTitle())
            .build();
    }

    private ExportDto createExportFile(Export export) throws ExperimentExportException {
        try {
            String exportFilename = String.format(
                Export.EXPORT_FILE_NAME,
                StringUtils.replace(experiment.getTitle(), " ", "_"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm"))
            );

            ExportDto transferExportDto = ExportDto.builder()
                .filename(String.format("%s.zip", exportFilename))
                .build();
            fileStorageService.createExperimentExportFile(transferExportDto, export, exportFilename);

            return transferExportDto;
        } catch (IOException e) {
            String error = String.format("Error occurred creating experiment ID: [%s] export", export.getExperiment().getId());
            log.error(error, e);
            throw new ExperimentExportException(error, e);
        }
    }

    private void prepareData(Experiment experiment) {
        this.experiment = experiment;
        this.treatments = treatmentRepository.findByCondition_Experiment_ExperimentIdOrderByCondition_ConditionIdAsc(experiment.getExperimentId());
        this.groups = groupRepository.findByExperiment_ExperimentId(experiment.getExperimentId());
        this.questions = questionRepository.findByAssessment_Treatment_Condition_Experiment_ExperimentId(experiment.getExperimentId());
        this.answersMc = answerMcRepository.findByQuestion_Assessment_Treatment_Condition_Experiment_ExperimentId(experiment.getExperimentId());
        this.exposureGroupConditions = exposureGroupConditionRepository.findByCondition_Experiment_ExperimentId(experiment.getExperimentId());
        this.assessments = this.treatments.stream()
            .map(Treatment::getAssessment)
            .toList();
        this.assignments = this.treatments.stream()
            .filter(distinctByKey(treatment -> treatment.getAssignment().getAssignmentId()))
            .map(Treatment::getAssignment)
            .toList();
        this.integrations = this.questions.stream()
            .filter(Question::isIntegration)
            .map(Question::getIntegration)
            .toList();
        this.outcomes = outcomeRepository.findByExposure_Experiment_ExperimentId(experiment.getExperimentId()).stream()
            .filter(outcome -> BooleanUtils.isNotTrue(outcome.getExternal()))
            .toList();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T,Object> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();

        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

}
