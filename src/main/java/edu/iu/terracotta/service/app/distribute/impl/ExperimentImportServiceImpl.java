package edu.iu.terracotta.service.app.distribute.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.AnswerMc;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.ExposureGroupCondition;
import edu.iu.terracotta.dao.entity.Group;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.entity.distribute.ExperimentImportError;
import edu.iu.terracotta.dao.entity.integrations.Integration;
import edu.iu.terracotta.dao.entity.integrations.IntegrationClient;
import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;
import edu.iu.terracotta.dao.model.distribute.export.Export;
import edu.iu.terracotta.dao.model.distribute.export.IntegrationExport;
import edu.iu.terracotta.dao.model.dto.distribute.ImportDto;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportErrorRepository;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportRepository;
import edu.iu.terracotta.exceptions.ExperimentImportException;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.async.ExperimentImportAsyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentImportErrorService;
import edu.iu.terracotta.service.app.distribute.ExperimentImportService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class ExperimentImportServiceImpl implements ExperimentImportService {

    @Autowired private ExperimentImportErrorRepository experimentImportErrorRepository;
    @Autowired private ExperimentImportRepository experimentImportRepository;
    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private ExperimentImportAsyncService experimentImportAsyncService;
    @Autowired private ExperimentImportErrorService experimentImportErrorService;
    @Autowired private FileStorageService fileStorageService;

    @Override
    public ImportDto preprocess(MultipartFile file, SecuredInfo securedInfo) throws ExperimentImportException {
        LtiUserEntity owner = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        LtiContextEntity context = ltiContextRepository.findById(securedInfo.getContextId())
            .orElseThrow(() -> new ExperimentImportException(String.format("Context ID: [%s] not found", securedInfo.getContextId())));

        try {
            ExperimentImport experimentImport = ExperimentImport.builder()
                    .context(context)
                    .fileName(file.getOriginalFilename())
                    .owner(owner)
                    .status(ExperimentImportStatus.PROCESSING)
                    .build();

            fileStorageService.saveExperimentImportFile(file, experimentImport);
            experimentImport = experimentImportRepository.save(experimentImport);

            validate(experimentImport);

            if (CollectionUtils.isNotEmpty(experimentImport.getErrors())) {
                // validation errors exists; skip processing
                for (ExperimentImportError experimentImportError : experimentImport.getErrors()) {
                    experimentImportError.setExperimentImport(experimentImport);
                    experimentImportErrorRepository.save(experimentImportError);
                }

                experimentImport.setStatus(ExperimentImportStatus.ERROR);
                experimentImport = experimentImportRepository.save(experimentImport);

                return toDto(experimentImport);
            }

            // start async import processing
            experimentImportAsyncService.process(experimentImport, securedInfo);

            return toDto(experimentImport);
        } catch (Exception e) {
            String error = String.format("Error importing experiment: owner ID: [%s], content ID: [%s]", securedInfo.getUserId(), securedInfo.getContextId());
            log.error(error, e);
            throw new ExperimentImportException(error, e);
        }
    }

    @Override
    public List<ImportDto> toDto(List<ExperimentImport> experimentImports) {
        return experimentImports.stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    public ImportDto toDto(ExperimentImport experimentImport) {
        return ImportDto.builder()
            .errorMessages(
                experimentImportErrorService.toDto(experimentImport.getErrors())
            )
            .id(experimentImport.getUuid())
            .importedTitle(experimentImport.getImportedTitle())
            .sourceTitle(experimentImport.getSourceTitle())
            .status(experimentImport.getStatus())
            .build();
    }

    @Override
    public ImportDto acknowledge(ExperimentImport experimentImport, ExperimentImportStatus experimentImportStatus) {
        if (experimentImport.isDeleted()) {
            // experiment import has been deleted; exit
            return toDto(experimentImport);
        }

        experimentImport.setStatus(experimentImportStatus);

        return toDto(experimentImportRepository.save(experimentImport));
    }

    @Override
    public List<ImportDto> getAll(SecuredInfo securedInfo) {
        return toDto(
            experimentImportRepository.findAllByOwner_UserKeyAndContext_ContextIdAndStatusIn(
                securedInfo.getUserId(),
                securedInfo.getContextId(),
                List.of(
                    ExperimentImportStatus.COMPLETE,
                    ExperimentImportStatus.ERROR,
                    ExperimentImportStatus.PROCESSING
                )
            )
        );
    }

    @Override
    public void validate(ExperimentImport experimentImport) {
        /*
         * Validate each experiment component.
         *
         * NOTE: Order of the validations is important!
         */

         try {
            Optional<Export> export = prepare(experimentImport);

            if (export.isEmpty()) {
                log.error("Experiment import ID: [{}] export object is null.", experimentImport.getId());
                handleError(experimentImport, "Unspecified error occurred processing import.");
            }

            experimentImport.setSourceTitle(export.get().getExperiment().getTitle());

            // ["component": ["imported id"]]
            Map<Class<? extends BaseEntity>, List<Long>> idMap = new HashMap<>();

            consentDocument(export.get(), experimentImport, export.get().getImportDirectory(), idMap);
            experiment(export.get(), experimentImport, idMap);
            conditions(export.get(), experimentImport, idMap);
            exposures(export.get(), experimentImport, idMap);
            groups(export.get(), experimentImport, idMap);
            exposureGroupConditions(export.get(), experimentImport, idMap);
            assignments(export.get(), experimentImport, idMap);
            treatments(export.get(), experimentImport, idMap);
            assessments(export.get(), experimentImport, idMap);
            questions(export.get(), experimentImport, idMap);
            integrationClients(export.get(), idMap);
            integrationConfigurations(export.get(), experimentImport, idMap);
            integrations(export.get(), experimentImport, idMap);
            answerMcs(export.get(), experimentImport, idMap);
            outcomes(export.get(), experimentImport, idMap);
         } catch (ExperimentImportException e) {
            log.warn("Validation exception occured for experiment import ID: [{}]. Exiting.", experimentImport.getId(), e);
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
            Export export = JsonMapper.builder()
                .build()
                .readValue(
                    jsonFile,
                    Export.class
                );
            export.setImportDirectory(importDirectory);

            return Optional.of(export);
        } catch (Exception e) {
            log.error("Error reading JSON file: [{}] for experiment import ID: [{}] ", jsonFile.getName(), experimentImport.getId(), e);
            handleError(experimentImport, String.format("Error reading JSON file: [%s]", jsonFile.getName()));
        }

        return Optional.empty();
    }

    private void consentDocument(Export export, ExperimentImport experimentImport, File importDirectory, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(ConsentDocument.class, new ArrayList<>());

        if (export.getExperiment().getParticipationType() == ParticipationTypes.CONSENT) {
            // experiment is consent type; process consent file
            File consentFile = FileUtils.getFile(importDirectory, String.format("/consent/%s", ExperimentImport.CONSENT_FILE_NAME));

            if (!consentFile.isFile()) {
                log.error("Experiment import ID: [{}] consent file : [{}] not found.", experimentImport.getId(), ExperimentImport.CONSENT_FILE_NAME);
                handleError(experimentImport, String.format("No consent PDF file [%s] found for experiment with consent participation type.", ExperimentImport.CONSENT_FILE_NAME));

                return;
            }

            idMap.get(ConsentDocument.class).add(export.getConsentDocument().getId());
        }
    }

    private void experiment(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        if (export.getExperiment() == null) {
            log.error("Experiment import ID: [{}] experiment is null.", experimentImport.getId());
            handleError(experimentImport, "No experiment found in import file.");
        }

        if (StringUtils.isBlank(export.getExperiment().getTitle())) {
            log.error("Experiment import ID: [{}] experiment title is null.", experimentImport.getId());
            handleError(experimentImport, "Experiment title cannot be blank.");
        }

        idMap.put(Experiment.class, List.of(export.getExperiment().getId()));
    }

    private void conditions(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Condition.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getConditions()).stream()
            .forEach(
                condition -> {
                    if (idMap.get(Experiment.class).stream().noneMatch(experimentId -> experimentId == condition.getExperimentId())) {
                        log.error("Experiment import ID: [{}] experiment ID: [{}] not found for condition ID: [{}]", experimentImport.getId(), condition.getExperimentId(), condition.getId());
                        handleError(experimentImport, String.format("No experiment ID: [%s] found for condition ID: [%s]", condition.getExperimentId(), condition.getId()));

                        return;
                    }

                    if (StringUtils.isBlank(condition.getName())) {
                        log.error("Experiment import ID: [{}] condition ID: [{}] name is null.", experimentImport.getId(), condition.getId());
                        handleError(experimentImport, String.format("Condition with ID: [%s] :: name cannot be blank.", condition.getId()));

                        return;
                    }

                    idMap.get(Condition.class).add(condition.getId());
                }
            );
    }

    private void exposures(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Exposure.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getExposures()).stream()
            .forEach(
                exposure -> {
                    if (idMap.get(Experiment.class).stream().noneMatch(experimentId -> experimentId == exposure.getExperimentId())) {
                        log.error("Experiment import ID: [{}] experiment ID: [{}] not found for exposure ID: [{}]", experimentImport.getId(), exposure.getExperimentId(), exposure.getId());
                        handleError(experimentImport, String.format("No experiment ID: [%s] found for exposure ID: [%s]", exposure.getExperimentId(), exposure.getId()));

                        return;
                    }

                    if (StringUtils.isBlank(exposure.getTitle())) {
                        log.error("Experiment import ID: [{}] exposure ID: [{}] title is null.", experimentImport.getId(), exposure.getId());
                        handleError(experimentImport, String.format("Exposure with ID: [%s] :: title cannot be blank.", exposure.getId()));

                        return;
                    }

                    idMap.get(Exposure.class).add(exposure.getId());
                }
            );
    }

    private void groups(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Group.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getGroups()).stream()
            .forEach(
                group -> {
                    if (idMap.get(Experiment.class).stream().noneMatch(experimentId -> experimentId == group.getExperimentId())) {
                        log.error("Experiment import ID: [{}] experiment ID: [{}] not found for group ID: [{}]", experimentImport.getId(), group.getExperimentId(), group.getId());
                        handleError(experimentImport, String.format("No experiment ID: [%s] found for group ID: [%s]", group.getExperimentId(), group.getId()));

                        return;
                    }

                    if (StringUtils.isBlank(group.getName())) {
                        log.error("Experiment import ID: [{}] group ID: [{}] name is null.", experimentImport.getId(), group.getId());
                        handleError(experimentImport, String.format("Group with ID: [%s] :: name cannot be blank.", group.getId()));

                        return;
                    }

                    idMap.get(Group.class).add(group.getId());
                }
            );
    }

    private void exposureGroupConditions(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(ExposureGroupCondition.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getExposureGroupConditions()).stream()
            .forEach(
                exposureGroupCondition -> {
                    if (idMap.get(Exposure.class).stream().noneMatch(exposureId -> exposureId == exposureGroupCondition.getExposureId())) {
                        log.error("Experiment import ID: [{}] exposure ID: [{}] not found for exposureGroupCondition ID: [{}]", experimentImport.getId(), exposureGroupCondition.getExposureId(), exposureGroupCondition.getId());
                        handleError(experimentImport, String.format("No exposure ID: [%s] found for exposureGroupCondition ID: [%s]", exposureGroupCondition.getExposureId(), exposureGroupCondition.getId()));

                        return;
                    }

                    if (idMap.get(Group.class).stream().noneMatch(groupId -> groupId == exposureGroupCondition.getGroupId())) {
                        log.error("Experiment import ID: [{}] group ID: [{}] not found for exposureGroupCondition ID: [{}]", experimentImport.getId(), exposureGroupCondition.getGroupId(), exposureGroupCondition.getId());
                        handleError(experimentImport, String.format("No group ID: [%s] found for exposureGroupCondition ID: [%s]", exposureGroupCondition.getGroupId(), exposureGroupCondition.getId()));

                        return;
                    }

                    if (idMap.get(Condition.class).stream().noneMatch(conditionId -> conditionId == exposureGroupCondition.getConditionId())) {
                        log.error("Experiment import ID: [{}] condition ID: [{}] not found for exposureGroupCondition ID: [{}]", experimentImport.getId(), exposureGroupCondition.getConditionId(), exposureGroupCondition.getId());
                        handleError(experimentImport, String.format("No condition ID: [%s] found for exposureGroupCondition ID: [%s]", exposureGroupCondition.getConditionId(), exposureGroupCondition.getId()));

                        return;
                    }

                    idMap.get(ExposureGroupCondition.class).add(exposureGroupCondition.getId());
                }
            );
    }

    private void assignments(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Assignment.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getAssignments()).stream()
            .forEach(
                assignment -> {
                    if (idMap.get(Exposure.class).stream().noneMatch(exposureId -> exposureId == assignment.getExposureId())) {
                        log.error("Experiment import ID: [{}] exposure ID: [{}] not found for exposureGroupCondition ID: [{}]", experimentImport.getId(), assignment.getExposureId(), assignment.getId());
                        handleError(experimentImport, String.format("No exposure ID: [%s] found for assignment ID: [%s]", assignment.getExposureId(), assignment.getId()));

                        return;
                    }

                    if (StringUtils.isBlank(assignment.getTitle())) {
                        log.error("Experiment import ID: [{}] assignment ID: [{}] title is null.", experimentImport.getId(), assignment.getId());
                        handleError(experimentImport, String.format("Assignment with ID: [%s] :: title cannot be blank.", assignment.getId()));

                        return;
                    }

                    idMap.get(Assignment.class).add(assignment.getId());
                }
            );
    }

    private void treatments(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Treatment.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getTreatments()).stream()
            .forEach(
                treatment -> {
                    if (idMap.get(Condition.class).stream().noneMatch(conditionId -> conditionId == treatment.getConditionId())) {
                        log.error("Experiment import ID: [{}] condition ID: [{}] not found for treatment ID: [{}]", experimentImport.getId(), treatment.getConditionId(), treatment.getId());
                        handleError(experimentImport, String.format("No condition ID: [%s] found for treatment ID: [%s]", treatment.getConditionId(), treatment.getId()));

                        return;
                    }

                    if (idMap.get(Assignment.class).stream().noneMatch(assignmentId -> assignmentId == treatment.getAssignmentId())) {
                        log.error("Experiment import ID: [{}] Assignment ID: [{}] not found for treatment ID: [{}]", experimentImport.getId(), treatment.getAssignmentId(), treatment.getId());
                        handleError(experimentImport, String.format("No assignment ID: [%s] found for treatment ID: [%s]", treatment.getAssignmentId(), treatment.getId()));

                        return;
                    }

                    idMap.get(Treatment.class).add(treatment.getId());
                }
            );
    }

    private void assessments(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Assessment.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getAssessments()).stream()
            .forEach(
                assessment -> {
                    if (idMap.get(Treatment.class).stream().noneMatch(treatmentId -> treatmentId == assessment.getTreatmentId())) {
                        log.error("Experiment import ID: [{}] treatment ID: [{}] not found for assessment ID: [{}]", experimentImport.getId(), assessment.getTreatmentId(), assessment.getId());
                        handleError(experimentImport, String.format("No treatment ID: [%s] found for assessment ID: [%s]", assessment.getTreatmentId(), assessment.getId()));

                        return;
                    }

                    idMap.get(Assessment.class).add(assessment.getId());
                }
            );
    }

    private void questions(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Question.class, new ArrayList<>());

        // check if any question is an integration type
        boolean hasIntegrationQuestions = CollectionUtils.emptyIfNull(export.getQuestions()).stream()
            .anyMatch(question -> question.getIntegrationId() != null);

        if (hasIntegrationQuestions) {
            // create temp map of integration IDs
            idMap.put(Integration.class, CollectionUtils.emptyIfNull(export.getIntegrations()).stream().map(IntegrationExport::getId).toList());
        }

        CollectionUtils.emptyIfNull(export.getQuestions()).stream()
            .forEach(
                question -> {
                    if (idMap.get(Assessment.class).stream().noneMatch(assessmentId -> assessmentId == question.getAssessmentId())) {
                        log.error("Experiment import ID: [{}] assessment ID: [{}] not found for question ID: [{}]", experimentImport.getId(), question.getAssessmentId(), question.getId());
                        handleError(experimentImport, String.format("No assessment ID: [%s] found for question ID: [%s]", question.getAssessmentId(), question.getId()));

                        return;
                    }

                    if (question.getIntegrationId() != null && idMap.get(Integration.class).stream().noneMatch(integrationId -> integrationId == question.getIntegrationId())) {
                        log.error("Experiment import ID: [{}] integration ID: [{}] not found for question ID: [{}]", experimentImport.getId(), question.getIntegrationId(), question.getId());
                        handleError(experimentImport, String.format("No integration ID: [%s] found for question ID: [%s]", question.getIntegrationId(), question.getId()));

                        return;
                    }

                    if (question.getQuestionOrder() == null) {
                        log.error("Experiment import ID: [{}] question ID: [{}] question order is null.", experimentImport.getId(), question.getId());
                        handleError(experimentImport, String.format("Question with ID: [%s] :: order cannot be null.", question.getId()));

                        return;
                    }

                    idMap.get(Question.class).add(question.getId());
                }
            );

            // reset integration IDs map
            idMap.put(Integration.class, new ArrayList<>());
    }

    private void integrationClients(Export export, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(IntegrationClient.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getIntegrationClients()).stream()
            .forEach(
                integrationClient -> {
                    idMap.get(IntegrationClient.class).add(integrationClient.getId());
                }
            );
    }

    private void integrationConfigurations(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(IntegrationConfiguration.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getIntegrationConfigurations()).stream()
            .forEach(
                integrationConfiguration -> {
                    if (idMap.get(IntegrationClient.class).stream().noneMatch(integrationClientId -> integrationClientId == integrationConfiguration.getClientId())) {
                        log.error("Experiment import ID: [{}] integration client ID: [{}] not found for integration configuration ID: [{}]", experimentImport.getId(), integrationConfiguration.getClientId(), integrationConfiguration.getId());
                        handleError(experimentImport, String.format("No integration client ID: [%s] found for integration configuration ID: [%s]", integrationConfiguration.getClientId(), integrationConfiguration.getId()));

                        return;
                    }

                    idMap.get(IntegrationConfiguration.class).add(integrationConfiguration.getId());
                }
            );
    }

    private void integrations(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(Integration.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getIntegrations()).stream()
            .forEach(
                integration -> {
                    if (idMap.get(IntegrationConfiguration.class).stream().noneMatch(integrationConfigurationId -> integrationConfigurationId == integration.getConfigurationId())) {
                        log.error("Experiment import ID: [{}] integration configuration ID: [{}] not found for integration ID: [{}]", experimentImport.getId(), integration.getConfigurationId(), integration.getId());
                        handleError(experimentImport, String.format("No integration configuration ID: [%s] found for integration ID: [%s]", integration.getConfigurationId(), integration.getId()));

                        return;
                    }

                    if (idMap.get(Question.class).stream().noneMatch(questionId -> questionId == integration.getQuestionId())) {
                        log.error("Experiment import ID: [{}] question ID: [{}] not found for integration ID: [{}]", experimentImport.getId(), integration.getQuestionId(), integration.getId());
                        handleError(experimentImport, String.format("No question ID: [%s] found for integration ID: [%s]", integration.getQuestionId(), integration.getId()));

                        return;
                    }

                    idMap.get(Integration.class).add(integration.getId());
                }
            );
    }

    private void answerMcs(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        idMap.put(AnswerMc.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getAnswersMc()).stream()
            .forEach(
                answerMc -> {
                    if (idMap.get(Question.class).stream().noneMatch(questionId -> questionId == answerMc.getQuestionId())) {
                        log.error("Experiment import ID: [{}] question ID: [{}] not found for answerMc ID: [{}]", experimentImport.getId(), answerMc.getQuestionId(), answerMc.getId());
                        handleError(experimentImport, String.format("No question ID: [%s] found for multiple choice answer ID: [%s]", answerMc.getQuestionId(), answerMc.getId()));

                        return;
                    }

                    if (answerMc.getAnswerOrder() == null) {
                        log.error("Experiment import ID: [{}] answerMc ID: [{}] answer order is null.", experimentImport.getId(), answerMc.getId());
                        handleError(experimentImport, String.format("AnswerMc with ID: [%s] :: order cannot be null.", answerMc.getId()));

                        return;
                    }

                    idMap.get(AnswerMc.class).add(answerMc.getId());
                }
            );
    }

    private void outcomes(Export export, ExperimentImport experimentImport, Map<Class<? extends BaseEntity>, List<Long>> idMap) {
        // process only non-external outcomes
        idMap.put(Outcome.class, new ArrayList<>());
        CollectionUtils.emptyIfNull(export.getOutcomes()).stream()
            .forEach(
                outcome -> {
                    if (idMap.get(Exposure.class).stream().noneMatch(exposureId -> exposureId == outcome.getExposureId())) {
                        log.error("Experiment import ID: [{}] exposure ID: [{}] not found for exposureGroupCondition ID: [{}]", experimentImport.getId(), outcome.getExposureId(), outcome.getId());
                        handleError(experimentImport, String.format("No exposure ID: [%s] found for exposureGroupCondition ID: [%s]", outcome.getExposureId(), outcome.getId()));

                        return;
                    }

                    idMap.get(Outcome.class).add(outcome.getId());
                }
            );
    }

    private void handleError(ExperimentImport experimentImport, String errorMessage) throws ExperimentImportException {
        experimentImport.setStatus(ExperimentImportStatus.ERROR);
        experimentImport.addErrorMessage(errorMessage);
        throw new ExperimentImportException(errorMessage);
    }

}
