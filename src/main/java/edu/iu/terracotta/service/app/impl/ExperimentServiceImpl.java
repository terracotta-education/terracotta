package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Condition;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.ConditionDto;
import edu.iu.terracotta.dao.model.dto.ConsentDto;
import edu.iu.terracotta.dao.model.dto.ExperimentDto;
import edu.iu.terracotta.dao.model.dto.ExposureDto;
import edu.iu.terracotta.dao.model.enums.DistributionTypes;
import edu.iu.terracotta.dao.model.enums.ExposureTypes;
import edu.iu.terracotta.dao.model.enums.ParticipationTypes;
import edu.iu.terracotta.dao.repository.ConditionRepository;
import edu.iu.terracotta.dao.repository.ConsentDocumentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ExposureRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentServiceImpl implements ExperimentService {

    @Autowired private ConditionRepository conditionRepository;
    @Autowired private ConsentDocumentRepository consentDocumentRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private ExposureRepository exposureRepository;
    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private AssignmentService assignmentService;
    @Autowired private AssignmentAsyncService asyncService;
    @Autowired private ConditionService conditionService;
    @Autowired private ExposureService exposureService;
    @Autowired private FileStorageServiceImpl fileStorageService;
    @Autowired private ParticipantService participantService;

    @Override
    public List<ExperimentDto> getExperiments(SecuredInfo securedInfo, boolean syncWithLms) {
        List<Experiment> experiments = experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(securedInfo.getPlatformDeploymentId(), securedInfo.getContextId());
        List<ExperimentDto> experimentDtoList = new ArrayList<>();

        for (Experiment experiment : experiments) {
            experimentDtoList.add(toDto(experiment, false, false, false, securedInfo));
        }

        // sync assignments with LMS, if configured
        if (syncWithLms) {
            try {
                log.info("Starting assignment sync in LMS.");
                asyncService.checkAndRestoreAssignmentsInLmsByContext(securedInfo);
                asyncService.handleObsoleteAssignmentsInLmsByContext(securedInfo);
                log.info("Assignment sync in LMS finished.");
            } catch (ApiException | DataServiceException | ConnectionException | IOException | TerracottaConnectorException e) {
                log.error("Error syncing assignments with LMS. Context ID: '{}'", securedInfo.getContextId(), e);
            }
        }

        return experimentDtoList;
    }

    @Override
    public Experiment getExperiment(long experimentId) {
        return experimentRepository.findByExperimentId(experimentId);
    }

    @Override
    public ExperimentDto postExperiment(ExperimentDto experimentDto, SecuredInfo securedInfo) throws DataServiceException, TitleValidationException {
        validateTitle(experimentDto.getTitle(), securedInfo.getContextId());

        Experiment experiment;
        experimentDto = fillContextInfo(experimentDto, securedInfo);

        try {
            experiment = fromDto(experimentDto);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create the experiment:" + e.getMessage(), e);
        }

        return toDto(save(experiment), false, false, false, securedInfo);
    }

    @Override
    public void updateExperiment(long experimentId, long contextId, ExperimentDto experimentDto, SecuredInfo securedInfo) throws TitleValidationException, WrongValueException, ParticipantNotUpdatedException, ExperimentNotMatchingException, IOException, NumberFormatException, TerracottaConnectorException {
        Experiment experimentToChange = getExperiment(experimentId);

        if (StringUtils.isAllBlank(experimentDto.getTitle(), experimentToChange.getTitle())) {
            throw new TitleValidationException("Error 100: Please give the experiment a title.");
        }

        if (StringUtils.isNotBlank(experimentDto.getTitle())) {
            if (experimentDto.getTitle().length() > 255) {
                throw new TitleValidationException("Error 101: Experiment title must be 255 characters or less.");
            }

            if (titleAlreadyExists(experimentDto.getTitle(), contextId, experimentId)) {
                throw new TitleValidationException("Error 102: Unable to create the experiment. An experiment with title \"" + experimentDto.getTitle() + "\" already exists in this course.");
            }
        }

        experimentToChange.setTitle(experimentDto.getTitle());
        experimentToChange.setDescription(experimentDto.getDescription());

        if (experimentToChange.isStarted() && !experimentDto.getExposureType().equals(experimentToChange.getExposureType().name())) {
            throw new WrongValueException("Error 110: The experiment has started. The Exposure Type can't be changed");
        }

        if (experimentToChange.isStarted() && !experimentDto.getDistributionType().equals(experimentToChange.getDistributionType().name())) {
            throw new WrongValueException("Error 110: The experiment has started. The Distribution Type can't be changed");
        }

        if (experimentToChange.isStarted()
                && !experimentDto.getParticipationType().equals(experimentToChange.getParticipationType().name())
                && experimentToChange.getParticipationType().equals(ParticipationTypes.CONSENT)) {
            throw new WrongValueException("Error 110: The experiment has started. The Participation Type can't be changed from 'Consent' to " + experimentDto.getParticipationType());
        }

        if (!experimentToChange.canSetExposureType()) {
            // cannot change exposure type after initial creation
            if (experimentToChange.getExposureType() != EnumUtils.getEnum(ExposureTypes.class, experimentDto.getExposureType())) {
                throw new WrongValueException("Error 110: The experiment has an existing exposure type. The Exposure Type can't be changed.");
            }
        }

        if (experimentDto.getExposureType() != null) {
            if (!EnumUtils.isValidEnum(ExposureTypes.class, experimentDto.getExposureType())) {
                throw new WrongValueException("Error 134: " + experimentDto.getExposureType() + " is not a valid Exposure value");
            }

            experimentToChange.setExposureType(EnumUtils.getEnum(ExposureTypes.class, experimentDto.getExposureType()));
        }

        if (experimentDto.getDistributionType() != null) {
            if (!EnumUtils.isValidEnum(DistributionTypes.class, experimentDto.getDistributionType())) {
                throw new WrongValueException("Error 134: " + experimentDto.getDistributionType() + " is not a valid Distribution value");
            }

            experimentToChange.setDistributionType(EnumUtils.getEnum(DistributionTypes.class, experimentDto.getDistributionType()));
        }

        if (experimentDto.getParticipationType() != null) {
            if (!EnumUtils.isValidEnum(ParticipationTypes.class, experimentDto.getParticipationType())) {
                throw new WrongValueException("Error 134: " + experimentDto.getParticipationType() + " is not a valid Participation value");
            }

            if (!experimentToChange.getParticipationType().name().equals(experimentDto.getParticipationType())) {
                if (ParticipationTypes.CONSENT.equals(experimentToChange.getParticipationType())) {
                    try {
                        fileStorageService.deleteConsentAssignment(experimentId, securedInfo);

                        if (experimentToChange.getConsentDocument()!=null) {
                            consentDocumentRepository.delete(experimentToChange.getConsentDocument());
                            experimentToChange.setConsentDocument(null);
                        }
                    } catch (ApiException | AssignmentNotEditedException e) {
                        log.warn("Consent from experiment {} was not deleted", experimentId);
                    }
                }

                changeParticipantionType(experimentDto.getParticipationType(),experimentId, securedInfo);
                experimentToChange.setParticipationType(EnumUtils.getEnum(ParticipationTypes.class, experimentDto.getParticipationType()));
            }
        }

        experimentToChange.setClosed(experimentDto.getClosed());
        experimentToChange.setStarted(experimentDto.getStarted());
        save(experimentToChange);
    }

    private void changeParticipantionType(String toPT, Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException, TerracottaConnectorException {
        switch (EnumUtils.getEnum(ParticipationTypes.class, toPT)) {
            case CONSENT:
            case NOSET:
                participantService.setAllToNull(experimentId, securedInfo);
                break;
            case AUTO:
                participantService.setAllToTrue(experimentId, securedInfo);
                break;
            case MANUAL:
                participantService.setAllToFalse(experimentId, securedInfo);
                break;
            default:
                break;
        }
    }

    @Override
    public ExperimentDto toDto(Experiment experiment, boolean conditions, boolean exposures, boolean participants, SecuredInfo securedInfo) {
        ExperimentDto experimentDto = new ExperimentDto();
        experimentDto.setExperimentId(experiment.getExperimentId());
        experimentDto.setContextId(experiment.getLtiContextEntity().getContextId());
        experimentDto.setPlatformDeploymentId(experiment.getPlatformDeployment().getKeyId());
        experimentDto.setTitle(experiment.getTitle());
        experimentDto.setDescription(experiment.getDescription());
        experimentDto.setExposureType(experiment.getExposureType().name());
        experimentDto.setParticipationType(experiment.getParticipationType().name());
        experimentDto.setDistributionType(experiment.getDistributionType().name());
        experimentDto.setStarted(experiment.getStarted());
        experimentDto.setClosed(experiment.getClosed());
        experimentDto.setCreatedAt(experiment.getCreatedAt());
        experimentDto.setUpdatedAt(experiment.getUpdatedAt());
        experimentDto.setCreatedBy(experiment.getCreatedBy().getUserId());
        List<ConditionDto> conditionDtoList = new ArrayList<>();

        if (conditions) {
            List<Condition> conditionList = conditionRepository.findByExperiment_ExperimentIdOrderByConditionIdAsc(experiment.getExperimentId());

            conditionDtoList = conditionList.stream()
                .map(condition -> conditionService.toDto(condition))
                .toList();
        }

        experimentDto.setConditions(conditionDtoList);

        List<ExposureDto> exposureDtoList = new ArrayList<>();

        if (exposures) {
            List<Exposure> exposureList = exposureRepository.findByExperiment_ExperimentId(experiment.getExperimentId());

            exposureDtoList = exposureList.stream()
                .map(exposure -> exposureService.toDto(exposure))
                .toList();
        }

        experimentDto.setExposures(exposureDtoList);

        if (participants) {
            List<Long> publishedExperimentAssignmentIds = participantService.calculatedPublishedAssignmentIds(experiment.getExperimentId(), securedInfo.getLmsCourseId(), experiment.getCreatedBy());

            experimentDto.setParticipants(
                CollectionUtils.emptyIfNull(participantRepository.findByExperiment_ExperimentId(experiment.getExperimentId())).stream()
                    .filter(participant -> !participant.isTestStudent())
                    .map(participant -> participantService.toDto(participant, publishedExperimentAssignmentIds, securedInfo))
                    .toList()
            );
        } else {
            experimentDto.setParticipants(Collections.emptyList());
        }

        int countAnswered = 0;
        int countAccepted = 0;
        int countRejected = 0;

        List<Participant> participantsList = CollectionUtils.emptyIfNull(experiment.getParticipants()).stream()
            .filter(participant -> !participant.getLtiUserEntity().isTestStudent())
            .toList();

        if (CollectionUtils.isNotEmpty(participantsList)) {
            experimentDto.setPotentialParticipants(participantsList.size());

            for (Participant participant : participantsList) {
                if (participant.getDateGiven() != null || participant.getDateRevoked() != null) {
                    countAnswered++;

                    if (participant.getConsent()) {
                        countAccepted++;
                    } else {
                        countRejected++;
                    }
                }
            }
        }

        experimentDto.setAcceptedParticipants(countAccepted);
        experimentDto.setRejectedParticipants(countRejected);

        ConsentDocument consentDocument = experiment.getConsentDocument();

        if (consentDocument != null) {
            ConsentDto consentDto = new ConsentDto();
            consentDto.setConsentDocumentId(consentDocument.getConsentDocumentId());
            consentDto.setFilePointer(consentDocument.getFilePointer());
            consentDto.setTitle(consentDocument.getTitle());
            consentDto.setHtml(fileStorageService.parseHTMLFiles(consentDocument.getHtml(), experiment.getPlatformDeployment().getLocalUrl()));
            consentDto.setExpectedConsent(participantsList.size());
            consentDto.setAnsweredConsentCount(countAnswered);
            experimentDto.setConsent(consentDto);
        }

        return experimentDto;
    }

    @Override
    public Experiment fromDto(ExperimentDto experimentDto) throws DataServiceException {
        Experiment experiment = new Experiment();
        experiment.setExperimentId(experimentDto.getExperimentId());
        Optional<LtiContextEntity> ltiContextEntity = ltiContextRepository.findById(experimentDto.getContextId());

        if (ltiContextEntity.isEmpty()) {
            throw new DataServiceException("The course defined in the experiment dto does not exist");
        }

        experiment.setLtiContextEntity(ltiContextEntity.get());
        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(experimentDto.getPlatformDeploymentId());

        if (platformDeployment.isEmpty()) {
            throw new DataServiceException("The platform deployment defined in the experiment dto does not exist");
        }

        experiment.setPlatformDeployment(platformDeployment.get());
        experiment.setTitle(experimentDto.getTitle());
        experiment.setDescription(experimentDto.getDescription());
        experiment.setExposureType(EnumUtils.getEnum(ExposureTypes.class, experimentDto.getExposureType(), ExposureTypes.NOSET));
        experiment.setParticipationType(EnumUtils.getEnum(ParticipationTypes.class, experimentDto.getParticipationType(), ParticipationTypes.NOSET));
        experiment.setDistributionType(EnumUtils.getEnum(DistributionTypes.class, experimentDto.getDistributionType(), DistributionTypes.NOSET));
        experiment.setStarted(experimentDto.getStarted());
        experiment.setClosed(experimentDto.getClosed());
        LtiUserEntity user = ltiUserRepository.findByUserIdAndPlatformDeployment_KeyId(experimentDto.getCreatedBy(),platformDeployment.get().getKeyId());

        if (user == null) {
            throw new DataServiceException("The user specified to create the experiment does not exist or does not belong to this course");
        }

        experiment.setCreatedBy(user);

        return experiment;
    }

    private Experiment save(Experiment experiment) {
        return experimentRepository.save(experiment);
    }

    @Override
    public void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException, IOException, TerracottaConnectorException {
        assignmentService.deleteAllFromExperiment(id, securedInfo);

        try {
            fileStorageService.deleteConsentAssignment(id, securedInfo);
        } catch (ApiException | AssignmentNotEditedException e) {
            log.warn("Consent from experiment {} was not deleted", id);
        }

        experimentRepository.deleteByExperimentId(id);
    }

    @Override
    public ExperimentDto fillContextInfo(ExperimentDto experimentDto, SecuredInfo securedInfo) {
        //Doing this ALWAYS for security reasons. In this way we will never edit or create experiments on other context
        //than the one in the token.
        experimentDto.setContextId(securedInfo.getContextId());
        experimentDto.setPlatformDeploymentId(securedInfo.getPlatformDeploymentId());
        LtiUserEntity user = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        if (user != null) {
            experimentDto.setCreatedBy(user.getUserId());
        }

        return experimentDto;
    }

    @Override
    public void deleteConsentDocument(ConsentDocument consentDocument) {
        consentDocumentRepository.delete(consentDocument);
    }

    @Override
    public ExperimentDto getEmptyExperiment(SecuredInfo securedInfo, ExperimentDto experimentDto) {
        if (StringUtils.isNotBlank(experimentDto.getTitle())) {
            return null;
        }

        List<Experiment> experimentList = experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndCreatedBy_UserKey(securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), securedInfo.getUserId());

        for (Experiment experiment : experimentList) {
            if (StringUtils.isBlank(experiment.getTitle())) {
                return toDto(experiment, false, false, false, securedInfo);
            }
        }

        return null;
    }

    private boolean titleAlreadyExists(String title, Long contextId, Long experimentId) {
        return experimentRepository.existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(title, contextId, experimentId);
    }

    @Override
    public void copyDto(ExperimentDto existingEmpty, ExperimentDto experimentDto) {
        existingEmpty.setDescription(experimentDto.getDescription());
        existingEmpty.setDistributionType(experimentDto.getDistributionType());
        existingEmpty.setParticipationType(experimentDto.getParticipationType());
        existingEmpty.setExposureType(experimentDto.getExposureType());
        existingEmpty.setTitle(experimentDto.getTitle());
        existingEmpty.setStarted(experimentDto.getStarted());
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiment/{id}").buildAndExpand(experimentId).toUri());

        return headers;
    }

    @Override
    public void validateTitle(String title, long contextId) throws TitleValidationException {
        if (StringUtils.isNotBlank(title)) {
            if (title.length() > 255) {
                throw new TitleValidationException("Error 101: Experiment title must be 255 characters or less.");
            }

            if (titleAlreadyExists(title, contextId, 0L)) {
                throw new TitleValidationException("Error 102: Unable to create the experiment. An experiment with title \"" + title + "\" already exists in this course.");
            }
        }
    }

}
