package edu.iu.terracotta.service.app.async.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.FileSubmissionLocal;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.QuestionMc;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.enums.LmsType;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.dao.repository.AnswerMcRepository;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ConsentDocumentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureGroupConditionRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.GroupRepository;
import edu.iu.terracotta.dao.repository.OutcomeRepository;
import edu.iu.terracotta.dao.repository.QuestionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationClientRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationConfigurationRepository;
import edu.iu.terracotta.dao.repository.integrations.IntegrationRepository;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.async.ExperimentImportAsyncService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentImportAsyncServiceImpl implements ExperimentImportAsyncService {

    @Autowired private AnswerMcRepository answerMcRepository;
    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ConsentDocumentRepository consentDocumentRepository;
    @Autowired private ExperimentImportRepository experimentImportRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureGroupConditionRepository exposureGroupConditionRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private IntegrationClientRepository integrationClientRepository;
    @Autowired private IntegrationConfigurationRepository integrationConfigurationRepository;
    @Autowired private IntegrationRepository integrationRepository;
    @Autowired private OutcomeRepository outcomeRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AssignmentService assignmentService;
    @Autowired private FileStorageService fileStorageService;

    @Async
    @Override
    @Transactional(rollbackFor = { Exception.class, ExperimentImportException.class })
    public void process(ExperimentImport experimentImport, SecuredInfo securedInfo) throws ExperimentImportException {
        log.info("Processing experiment import with ID: [{}]", experimentImport.getId());
        Optional<Export> export = prepare(experimentImport);

        if (export.isEmpty()) {
            log.error("Experiment import ID: [{}] export object is null.", experimentImport.getId());
            handleError(experimentImport, "No import file found.");

            return;
        }

        if (CollectionUtils.isNotEmpty(experimentImport.getErrors())) {
            log.error("Experiment import ID: [{}] has vaidation errors: [{}]", experimentImport.getId(), StringUtils.join(experimentImport.getErrors(), ","));
            handleError(experimentImport, String.format("Validation errors: [%s]", StringUtils.join(experimentImport.getErrors(), ",")));

            return;
        }

        // ["component": ["imported id": "newly-created object"]]
        Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap = new HashMap<>();

        /*
         * Process each experiment component.
         *
         * NOTE: Order of the imports is important!
         */

        consentDocument(export.get(), experimentImport, export.get().getImportDirectory(), idMap);
        experiment(export.get(), experimentImport, idMap);
        conditions(export.get(), idMap);
        exposures(export.get(), idMap);
        groups(export.get(), idMap);
        exposureGroupConditions(export.get(), idMap);
        assignments(export.get(), idMap);
        treatments(export.get(), idMap);
        assessments(export.get(), idMap);
        questions(export.get(), idMap);
        integrationClients(export.get(), idMap);
        integrationConfigurations(export.get(), idMap);
        integrations(export.get(), idMap);
        answerMcs(export.get(), idMap);
        outcomes(export.get(), idMap);

        if (CollectionUtils.isEmpty(experimentImport.getErrors())) {
            // no errors occurred yet; create assignments in LMS
            sendAssignmentsToLms(export.get(), experimentImport, idMap, securedInfo);
        }

        if (CollectionUtils.isEmpty(experimentImport.getErrors())) {
            // import completed without errors; set status to complete
            experimentImport.setStatus(ExperimentImportStatus.COMPLETE);
            experimentImportRepository.save(experimentImport);
        }

        log.info("Processing experiment import with ID: [{}] COMPLETE", experimentImport.getId());

        if (CollectionUtils.isNotEmpty(experimentImport.getErrors())) {
            // errors occurred; throw exception to rollback transactions
            throw new ExperimentImportException(String.format("Errors occurred processing experiment import with ID: [%s]. Rolling back transactions.", experimentImport.getId()));
        }
    }

    private Optional<Export> prepare(ExperimentImport experimentImport) {
        File importDirectory = fileStorageService.getExperimentImportFile(experimentImport.getId());

        if (importDirectory == null) {
            log.error("Experiment import ID: [{}] file : [{}] not found.", experimentImport.getId(), experimentImport.getFileUri());
            handleError(experimentImport, "No import .zip file found.");

            return Optional.empty();
        }

        // get import JSON file
        File jsonFile = FileUtils.getFile(importDirectory, ExperimentImport.JSON_FILE_NAME);

        if (!jsonFile.isFile()) {
            log.error("Experiment import ID: [{}] JSON file : [{}] not found.", experimentImport.getId(), ExperimentImport.JSON_FILE_NAME);
            handleError(experimentImport, String.format("No JSON file [%s] found in imported .zip file.", ExperimentImport.JSON_FILE_NAME));

            return Optional.empty();
        }

        try {
            Export export = new ObjectMapper().readValue(jsonFile, Export.class);
            export.setImportDirectory(importDirectory);

            return Optional.of(export);
        } catch (IOException e) {
            log.error("Error reading JSON file: [{}] for experiment import ID: [{}] ", jsonFile.getName(), experimentImport.getId(), e);
            handleError(experimentImport, String.format("Error reading JSON file: [%s]", jsonFile.getName()));
        }

        return Optional.empty();
    }

    private void consentDocument(Export export, ExperimentImport experimentImport, File importDirectory, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(ConsentDocument.class, new HashMap<>());

        if (export.getExperiment().getParticipationType() == ParticipationTypes.CONSENT) {
            // experiment is consent type; process consent file
            File consentFile = FileUtils.getFile(importDirectory, String.format("/consent/%s", ExperimentImport.CONSENT_FILE_NAME));

            if (!consentFile.isFile()) {
                log.error("Experiment import ID: [{}] consent file : [{}] not found.", experimentImport.getId(), ExperimentImport.CONSENT_FILE_NAME);
                handleError(experimentImport, String.format("No consent PDF file [%s] found for experiment with consent participation type.", ExperimentImport.CONSENT_FILE_NAME));
                return;
            }

            try (InputStream inputStream = new FileInputStream(consentFile)) {
                FileSubmissionLocal fileSubmissionLocal = fileStorageService.saveConsentFile(inputStream, consentFile.getName());

                ConsentDocument consentDocument = consentDocumentRepository.save(
                    ConsentDocument.builder()
                        .encryptionMethod(fileSubmissionLocal.getEncryptionMethod())
                        .encryptionPhrase(fileSubmissionLocal.getEncryptionPhrase())
                        .fileUri(fileSubmissionLocal.getFilePath())
                        .html(export.getConsentDocument().getHtml())
                        .lmsAssignmentId(null)
                        .resourceLinkId(null)
                        .title(export.getConsentDocument().getTitle())
                        .build()
                );
                idMap.get(ConsentDocument.class).put(export.getConsentDocument().getId(), consentDocument);
            } catch (IOException e) {
                log.error("Experiment import ID: [{}] error processing consent file : [{}].", experimentImport.getId(), ExperimentImport.CONSENT_FILE_NAME);
                handleError(experimentImport, String.format("Error processing consent PDF file [%s].", ExperimentImport.CONSENT_FILE_NAME));
            }
        }
    }

    private void experiment(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        List<String> existingExperiments = experimentRepository.findAll().stream()
            .map(Experiment::getTitle)
            .toList();

        String title = String.format("%s %s", ExperimentImport.EXPERIMENT_TITLE_PREFIX, export.getExperiment().getTitle());
        int index = 1;

        // ensure experiment title does not exist already
        while (existingExperiments.contains(title)) {
            title = String.format("%s %s (%s)", ExperimentImport.EXPERIMENT_TITLE_PREFIX, export.getExperiment().getTitle(), index++);
        }

        Experiment experiment = experimentRepository.save(
            Experiment.builder()
                .consentDocument(
                    ParticipationTypes.CONSENT == export.getExperiment().getParticipationType() ?
                        (ConsentDocument) idMap.get(ConsentDocument.class).get(export.getConsentDocument().getId())
                        :
                        null
                )
                .createdBy(experimentImport.getOwner())
                .description(export.getExperiment().getDescription())
                .distributionType(export.getExperiment().getDistributionType())
                .exposureType(export.getExperiment().getExposureType())
                .ltiContextEntity(experimentImport.getContext())
                .participationType(export.getExperiment().getParticipationType())
                .platformDeployment(experimentImport.getOwner().getPlatformDeployment())
                .title(title)
                .build()
        );
        idMap.put(Experiment.class, Collections.singletonMap(export.getExperiment().getId(), experiment));
        experimentImport.setImportedTitle(title);
    }

    private void conditions(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Condition.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getConditions()).stream()
            .map(
                condition -> {
                    Condition newCondition = conditionRepository.save(
                        Condition.builder()
                            .defaultCondition(condition.getDefaultCondition())
                            .distributionPct(condition.getDistributionPct())
                            .experiment((Experiment) idMap.get(Experiment.class).get(condition.getExperimentId()))
                            .name(condition.getName())
                            .build()
                    );
                    idMap.get(Condition.class).put(condition.getId(), newCondition);

                    return newCondition;
                }
            )
            .toList();
    }

    private void exposures(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Exposure.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getExposures()).stream()
            .map(
                exposure -> {
                    Exposure newExposure = exposureRepository.save(
                        Exposure.builder()
                            .experiment((Experiment) idMap.get(Experiment.class).get(exposure.getExperimentId()))
                            .title(exposure.getTitle())
                            .build()
                    );

                    idMap.get(Exposure.class).put(exposure.getId(), newExposure);

                    return newExposure;
                }
            )
            .toList();
    }

    private void groups(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Group.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getGroups()).stream()
            .map(
                group -> {
                    Group newGroup = groupRepository.save(
                        Group.builder()
                            .experiment((Experiment) idMap.get(Experiment.class).get(group.getExperimentId()))
                            .name(group.getName())
                            .build()
                    );
                    idMap.get(Group.class).put(group.getId(), newGroup);

                    return newGroup;
                }
            )
            .toList();
    }

    private void exposureGroupConditions(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(ExposureGroupCondition.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getExposureGroupConditions()).stream()
            .map(
                exposureGroupCondition -> {
                    ExposureGroupCondition newExposureGroupCondition = exposureGroupConditionRepository.save(
                        ExposureGroupCondition.builder()
                            .condition((Condition) idMap.get(Condition.class).get(exposureGroupCondition.getConditionId()))
                            .exposure((Exposure) idMap.get(Exposure.class).get(exposureGroupCondition.getExposureId()))
                            .group((Group) idMap.get(Group.class).get(exposureGroupCondition.getGroupId()))
                            .build()
                    );

                    idMap.get(ExposureGroupCondition.class).put(exposureGroupCondition.getId(), newExposureGroupCondition);

                    return newExposureGroupCondition;
                }
            )
            .toList();
    }

    private void assignments(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Assignment.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getAssignments()).stream()
            .map(
                assignment -> {
                    Assignment newAssignment = assignmentRepository.save(
                        Assignment.builder()
                            .allowStudentViewCorrectAnswers(assignment.isAllowStudentViewCorrectAnswers())
                            .allowStudentViewResponses(assignment.isAllowStudentViewResponses())
                            .assignmentOrder(assignment.getAssignmentOrder())
                            .cumulativeScoringInitialPercentage(assignment.getCumulativeScoringInitialPercentage())
                            .exposure((Exposure) idMap.get(Exposure.class).get(assignment.getExposureId()))
                            .hoursBetweenSubmissions(assignment.getHoursBetweenSubmissions())
                            .lmsAssignmentId(null)
                            .multipleSubmissionScoringScheme(assignment.getMultipleSubmissionScoringScheme())
                            .numOfSubmissions(assignment.getNumOfSubmissions())
                            .published(false)
                            .resourceLinkId(null)
                            .studentViewCorrectAnswersAfter(assignment.getStudentViewCorrectAnswersAfter())
                            .studentViewCorrectAnswersBefore(assignment.getStudentViewCorrectAnswersBefore())
                            .studentViewResponsesAfter(assignment.getStudentViewResponsesAfter())
                            .studentViewResponsesBefore(assignment.getStudentViewResponsesBefore())
                            .title(assignment.getTitle())
                            .build()
                    );
                    idMap.get(Assignment.class).put(assignment.getId(), newAssignment);

                    return newAssignment;
                }
            )
            .toList();
    }

    private void treatments(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Treatment.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getTreatments()).stream()
            .map(
                treatment -> {
                    Treatment newTreatment = treatmentRepository.save(
                        Treatment.builder()
                            .assignment((Assignment) idMap.get(Assignment.class).get(treatment.getAssignmentId()))
                            .condition((Condition) idMap.get(Condition.class).get(treatment.getConditionId()))
                            .build()
                    );

                    idMap.get(Treatment.class).put(treatment.getId(), newTreatment);

                    return newTreatment;
                }
            )
            .toList();
    }

    private void assessments(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Assessment.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getAssessments()).stream()
            .map(
                assessment -> {
                    Treatment treatment = (Treatment) idMap.get(Treatment.class).get(assessment.getTreatmentId());

                    Assessment newAssessment = assessmentRepository.save(
                        Assessment.builder()
                            .allowStudentViewCorrectAnswers(assessment.isAllowStudentViewCorrectAnswers())
                            .allowStudentViewResponses(assessment.isAllowStudentViewResponses())
                            .autoSubmit(assessment.isAutoSubmit())
                            .cumulativeScoringInitialPercentage(assessment.getCumulativeScoringInitialPercentage())
                            .hoursBetweenSubmissions(assessment.getHoursBetweenSubmissions())
                            .html(assessment.getHtml())
                            .multipleSubmissionScoringScheme(assessment.getMultipleSubmissionScoringScheme())
                            .numOfSubmissions(assessment.getNumOfSubmissions())
                            .studentViewCorrectAnswersAfter(assessment.getStudentViewCorrectAnswersAfter())
                            .studentViewCorrectAnswersBefore(assessment.getStudentViewCorrectAnswersBefore())
                            .studentViewResponsesAfter(assessment.getStudentViewResponsesAfter())
                            .studentViewResponsesBefore(assessment.getStudentViewResponsesBefore())
                            .title(assessment.getTitle())
                            .treatment(treatment)
                            .build()
                    );

                    idMap.get(Assessment.class).put(assessment.getId(), newAssessment);

                    // save assessment to treatment
                    treatment.setAssessment(newAssessment);
                    treatmentRepository.save(treatment);

                    return newAssessment;
                }
            )
            .toList();
    }

    private void questions(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Question.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getQuestions()).stream()
            .map(
                question -> {
                    Question newQuestion = Question.builder().build();

                    if (QuestionTypes.MC == question.getQuestionType()) {
                        QuestionMc newQuestionMc = new QuestionMc();
                        newQuestionMc.setRandomizeAnswers(question.isRandomizeAnswers());
                        newQuestion = newQuestionMc;
                    }

                    newQuestion.setAssessment((Assessment) idMap.get(Assessment.class).get(question.getAssessmentId()));
                    newQuestion.setHtml(question.getHtml());
                    newQuestion.setPoints(question.getPoints());
                    newQuestion.setQuestionOrder(question.getQuestionOrder());
                    newQuestion.setQuestionType(question.getQuestionType());
                    newQuestion.setRegradeOption(question.getRegradeOption());

                    newQuestion = questionRepository.save(newQuestion);
                    idMap.get(Question.class).put(question.getId(), newQuestion);

                    return newQuestion;
                }
            )
            .toList();
    }

    private void integrationClients(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        List<IntegrationClient> existingIntegrationClients = integrationClientRepository.findAll();

        // process only integration clients that do not have the same name
        idMap.put(IntegrationClient.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getIntegrationClients()).stream()
            .map(
                integrationClient -> {
                    // find integration client with the same name and is enabled
                    IntegrationClient newIntegrationClient = existingIntegrationClients.stream()
                        .filter(IntegrationClient::isEnabled)
                        .filter(existingClient -> Strings.CI.equals(existingClient.getName(), integrationClient.getName()))
                        .findFirst()
                        .orElse(null);

                    if (newIntegrationClient == null) {
                        // integration client does not exist; create new
                        newIntegrationClient = integrationClientRepository.save(
                            IntegrationClient.builder()
                                .enabled(integrationClient.isEnabled())
                                .name(integrationClient.getName())
                                .previewToken(integrationClient.getPreviewToken())
                                .scoreVariable(integrationClient.getScoreVariable())
                                .tokenVariable(integrationClient.getTokenVariable())
                                .build()
                        );
                    }

                    idMap.get(IntegrationClient.class).put(integrationClient.getId(), newIntegrationClient);

                    return newIntegrationClient;
                }
            )
            .toList();
    }

    private void integrationConfigurations(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(IntegrationConfiguration.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getIntegrationConfigurations()).stream()
            .map(
                integrationConfiguration -> {
                    IntegrationConfiguration newIntegrationConfiguration = integrationConfigurationRepository.save(
                        IntegrationConfiguration.builder()
                            .client((IntegrationClient) idMap.get(IntegrationClient.class).get(integrationConfiguration.getClientId()))
                            .launchUrl(integrationConfiguration.getLaunchUrl())
                            .build()
                    );

                    idMap.get(IntegrationConfiguration.class).put(integrationConfiguration.getId(), newIntegrationConfiguration);

                    return newIntegrationConfiguration;
                }
            )
            .toList();
    }

    private void integrations(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(Integration.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getIntegrations()).stream()
            .map(
                integration -> {
                    Integration newIntegration = integrationRepository.save(
                        Integration.builder()
                            .configuration((IntegrationConfiguration) idMap.get(IntegrationConfiguration.class).get(integration.getConfigurationId()))
                            .question((Question) idMap.get(Question.class).get(integration.getQuestionId()))
                            .build()
                    );

                    idMap.get(Integration.class).put(integration.getId(), newIntegration);

                    return newIntegration;
                }
            )
            .toList();
    }

    private void answerMcs(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        idMap.put(AnswerMc.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getAnswersMc()).stream()
            .map(
                answerMc -> {
                    AnswerMc newAnswerMc = answerMcRepository.save(
                        AnswerMc.builder()
                            .answerOrder(answerMc.getAnswerOrder())
                            .correct(answerMc.getCorrect())
                            .html(answerMc.getHtml())
                            .question((Question) idMap.get(Question.class).get(answerMc.getQuestionId()))
                            .build()
                    );

                    idMap.get(AnswerMc.class).put(answerMc.getId(), newAnswerMc);

                    return newAnswerMc;
                }
            )
            .toList();
    }

    private void outcomes(Export export, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap) {
        // process only non-external outcomes
        idMap.put(Outcome.class, new HashMap<>());
        CollectionUtils.emptyIfNull(export.getOutcomes()).stream()
            .map(
                outcome -> {
                    Outcome newOutcome = outcomeRepository.save(
                        Outcome.builder()
                            .exposure((Exposure) idMap.get(Exposure.class).get(outcome.getExposureId()))
                            .external(false)
                            .lmsType(LmsType.none)
                            .maxPoints(outcome.getMaxPoints())
                            .title(outcome.getTitle())
                            .build()
                    );

                    idMap.get(Outcome.class).put(outcome.getId(), newOutcome);

                    return newOutcome;
                }
            )
            .toList();
    }

    private void sendAssignmentsToLms(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, Map<Long, BaseEntity>> idMap, SecuredInfo securedInfo) {
        if (MapUtils.isEmpty(idMap.get(Assignment.class))) {
            // no assignments to process
            return;
        }

        idMap.get(Assignment.class).entrySet().stream()
            .forEach(
                entry -> {
                    Assignment assignment = (Assignment) entry.getValue();
                    try {
                        assignmentService.createAssignmentInLms(
                            experimentImport.getOwner(),
                            assignment,
                            ((Experiment) idMap.get(Experiment.class).get(export.getExperiment().getId())).getExperimentId(),
                            securedInfo.getLmsCourseId()
                        );
                    } catch (AssignmentNotCreatedException | TerracottaConnectorException e) {
                        log.error("Error processing experiment import with ID: [{}]. Assignment creation in LMS failed.", experimentImport.getUuid(), e);
                        handleError(experimentImport, "Assignment creation in LMS failed");
                    }
                }
            );

        if (CollectionUtils.isEmpty(experimentImport.getErrors())) {
            if (MapUtils.isNotEmpty(idMap.get(ConsentDocument.class))) {
                try {
                    fileStorageService.sendConsentFileToLms(
                        (ConsentDocument) idMap.get(ConsentDocument.class).get(export.getConsentDocument().getId()),
                        (Experiment) idMap.get(Experiment.class).get(export.getExperiment().getId()),
                        experimentImport.getOwner());
                } catch (AssignmentNotCreatedException | IOException | TerracottaConnectorException e) {
                    log.error("Error processing experiment import with ID: [{}]. Consent assignment creation in LMS failed.", experimentImport.getUuid(), e);
                    handleError(experimentImport, "Consent assignment creation in LMS failed");
                }
            }
        }
    }

    private void handleError(ExperimentImport experimentImport, String errorMessage) {
        experimentImport.setStatus(ExperimentImportStatus.ERROR);
        experimentImport.addErrorMessage(errorMessage);
    }

}
