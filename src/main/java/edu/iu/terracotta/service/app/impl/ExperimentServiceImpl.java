package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssignmentNotEditedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.model.LtiContextEntity;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import edu.iu.terracotta.model.app.dto.ConsentDto;
import edu.iu.terracotta.model.app.dto.ExperimentDto;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.model.app.dto.ParticipantDto;
import edu.iu.terracotta.model.app.enumerator.DistributionTypes;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.model.app.enumerator.ParticipationTypes;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.ParticipantService;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentServiceImpl implements ExperimentService {

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private ConditionService conditionService;

    @Autowired
    private ExposureService exposureService;

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private FileStorageServiceImpl fileStorageService;

    @Override
    public List<Experiment> findAllByDeploymentIdAndCourseId(long deploymentId, long contextId) {
        return allRepositories.experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(deploymentId,contextId);
    }

    @Override
    public List<ExperimentDto> getExperiments(SecuredInfo securedInfo, boolean syncWithCanvas) {
        List<Experiment> experiments = findAllByDeploymentIdAndCourseId(securedInfo.getPlatformDeploymentId(), securedInfo.getContextId());
        List<ExperimentDto> experimentDtoList = new ArrayList<>();

        for (Experiment experiment : experiments) {
            experimentDtoList.add(toDto(experiment, false, false,  false));
        }

        // sync assignments with Canvas, if configured
        if (syncWithCanvas) {
            Thread thread = new Thread(
                () ->
                    {
                        try {
                            assignmentService.checkAndRestoreAssignmentsInCanvasByContext(securedInfo);
                        } catch (CanvasApiException | DataServiceException | ConnectionException | IOException e) {
                            log.error("Error syncing assignments with Canvas. Context ID: '{}'", securedInfo.getContextId(), e);
                        }
                    }
            );
            thread.start();
        }

        return experimentDtoList;
    }

    @Override
    public Experiment getExperiment(long experimentId) {
        return allRepositories.experimentRepository.findByExperimentId(experimentId);
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

        return toDto(save(experiment), false, false, false);
    }

    @Override
    public void updateExperiment(long experimentId, long contextId, ExperimentDto experimentDto, SecuredInfo securedInfo) throws TitleValidationException, WrongValueException, ParticipantNotUpdatedException, ExperimentNotMatchingException {
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
                            allRepositories.consentDocumentRepository.delete(experimentToChange.getConsentDocument());
                            experimentToChange.setConsentDocument(null);
                        }
                    } catch (CanvasApiException | AssignmentNotEditedException e) {
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

    private void changeParticipantionType(String toPT, Long experimentId, SecuredInfo securedInfo) throws ParticipantNotUpdatedException, ExperimentNotMatchingException {
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
    public Optional<Experiment> findOneByDeploymentIdAndCourseIdAndExperimentId(long deploymentId, long contextId, long id) {
        return allRepositories.experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndExperimentId(deploymentId,contextId, id);
    }

    @Override
    public ExperimentDto toDto(Experiment experiment, boolean conditions, boolean exposures, boolean participants) {
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
            List<Condition> conditionList = allRepositories.conditionRepository.findByExperiment_ExperimentId(experiment.getExperimentId());

            for (Condition condition:conditionList) {
                ConditionDto conditionDto = conditionService.toDto(condition);
                conditionDtoList.add(conditionDto);
            }
        }

        experimentDto.setConditions(conditionDtoList);

        List<ExposureDto> exposureDtoList = new ArrayList<>();

        if (exposures) {
            List<Exposure> exposureList = allRepositories.exposureRepository.findByExperiment_ExperimentId(experiment.getExperimentId());

            for (Exposure exposure : exposureList) {
                ExposureDto exposureDto = exposureService.toDto(exposure);
                exposureDtoList.add(exposureDto);
            }
        }
        experimentDto.setExposures(exposureDtoList);

        List<ParticipantDto> participantDtoList = new ArrayList<>();

        if (participants) {
            List<Participant> participantList = allRepositories.participantRepository.findByExperiment_ExperimentId(experiment.getExperimentId());

            for (Participant participant : participantList) {
                ParticipantDto participantDto = participantService.toDto(participant);
                participantDtoList.add(participantDto);
            }
        }

        experimentDto.setParticipants(participantDtoList);
        int countAnswered = 0;
        int countAccepted = 0;
        int countRejected = 0;

        if (experiment.getParticipants() != null) {
            experimentDto.setPotentialParticipants(experiment.getParticipants().size());

            for (Participant participant : experiment.getParticipants()) {
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
            consentDto.setExpectedConsent(experiment.getParticipants().size());
            consentDto.setAnsweredConsentCount(countAnswered);
            experimentDto.setConsent(consentDto);
        }

        return experimentDto;
    }

    @Override
    public Experiment fromDto(ExperimentDto experimentDto) throws DataServiceException {
        Experiment experiment = new Experiment();
        experiment.setExperimentId(experimentDto.getExperimentId());
        Optional<LtiContextEntity> ltiContextEntity = allRepositories.contexts.findById(experimentDto.getContextId());

        if (!ltiContextEntity.isPresent()) {
            throw new DataServiceException("The course defined in the experiment dto does not exist");
        }

        experiment.setLtiContextEntity(ltiContextEntity.get());
        Optional<PlatformDeployment> platformDeployment = allRepositories.platformDeploymentRepository.findById(experimentDto.getPlatformDeploymentId());

        if (platformDeployment.isPresent()) {
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
        LtiUserEntity user = allRepositories.users.findByUserIdAndPlatformDeployment_KeyId(experimentDto.getCreatedBy(),platformDeployment.get().getKeyId());

        if (user == null) {
            throw new DataServiceException("The user specified to create the experiment does not exist or does not belong to this course");
        }

        experiment.setCreatedBy(user);

        return experiment;
    }

    @Override
    public Experiment save(Experiment experiment) {
        return allRepositories.experimentRepository.save(experiment);
    }

    @Override
    public Optional<Experiment> findById(Long id) {
        return allRepositories.experimentRepository.findById(id);
    }

    @Override
    public void saveAndFlush(Experiment experimentToChange) {
        allRepositories.experimentRepository.saveAndFlush(experimentToChange);
    }

    @Override
    public void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException {
        assignmentService.deleteAllFromExperiment(id, securedInfo);

        try {
            fileStorageService.deleteConsentAssignment(id, securedInfo);
        } catch (CanvasApiException | AssignmentNotEditedException e) {
            log.warn("Consent from experiment {} was not deleted", id);
        }

        allRepositories.experimentRepository.deleteByExperimentId(id);
    }

    @Override
    public ExperimentDto fillContextInfo(ExperimentDto experimentDto, SecuredInfo securedInfo) {
        //Doing this ALWAYS for security reasons. In this way we will never edit or create experiments on other context
        //than the one in the token.
        experimentDto.setContextId(securedInfo.getContextId());
        experimentDto.setPlatformDeploymentId(securedInfo.getPlatformDeploymentId());
        LtiUserEntity user = allRepositories.users.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());

        if (user != null) {
            experimentDto.setCreatedBy(user.getUserId());
        }

        return experimentDto;
    }

    @Override
    public boolean experimentBelongsToDeploymentAndCourse(Long experimentId, Long platformDeploymentId, Long contextId) {
        return allRepositories.experimentRepository.existsByExperimentIdAndPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(experimentId, platformDeploymentId, contextId);
    }

    @Override
    public ConsentDocument saveConsentDocument(ConsentDocument consentDocument) {
        return allRepositories.consentDocumentRepository.save(consentDocument);
    }

    @Override
    public void deleteConsentDocument(ConsentDocument consentDocument) {
        allRepositories.consentDocumentRepository.delete(consentDocument);
    }

    @Override
    public ExperimentDto getEmptyExperiment(SecuredInfo securedInfo, ExperimentDto experimentDto) {
        if (StringUtils.isNotBlank(experimentDto.getTitle())) {
            return null;
        }

        List<Experiment> experimentList = allRepositories.experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndCreatedBy_UserKey(securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), securedInfo.getUserId());

        for (Experiment experiment : experimentList) {
            if (StringUtils.isBlank(experiment.getTitle())) {
                return toDto(experiment, false, false, false);
            }
        }

        return null;
    }

    @Override
    public boolean titleAlreadyExists(String title, Long contextId, Long experimentId) {
        return allRepositories.experimentRepository.existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(title, contextId, experimentId);
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
