package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
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
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.ParticipantService;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExperimentServiceImpl implements ExperimentService {


    @Autowired
    AllRepositories allRepositories;

    @Autowired
    ConditionService conditionService;

    @Autowired
    ExposureService exposureService;

    @Autowired
    ParticipantService participantService;

    @Override
    public List<Experiment> findAllByDeploymentIdAndCourseId(long deploymentId, long contextId) {
        return allRepositories.experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextId(deploymentId,contextId);
    }

    @Override
    public List<ExperimentDto> getExperiments(long deploymentId, long contextId){
        List<Experiment> experiments = findAllByDeploymentIdAndCourseId(deploymentId, contextId);
        List<ExperimentDto> experimentDtoList = new ArrayList<>();
        for(Experiment experiment : experiments){
            experimentDtoList.add(toDto(experiment, false, false,  false));
        }
        return experimentDtoList;
    }

    @Override
    public Experiment getExperiment(long experimentId){ return allRepositories.experimentRepository.findByExperimentId(experimentId); }

    @Override
    public void updateExperiment(long experimentId, long contextId, ExperimentDto experimentDto) throws TitleValidationException, WrongValueException{
        Experiment experimentToChange = getExperiment(experimentId);
        if(StringUtils.isAllBlank(experimentDto.getTitle()) && StringUtils.isAllBlank(experimentToChange.getTitle())){
            throw new TitleValidationException("Error 100: Please give the experiment a title.");
        }
        if(!StringUtils.isBlank(experimentDto.getTitle())){
            if(experimentDto.getTitle().length() > 255){
                throw new TitleValidationException("Error 101: Experiment title must be 255 characters or less.");
            }
            if(titleAlreadyExists(experimentDto.getTitle(), contextId, experimentId)){
                throw new TitleValidationException("Error 102: Unable to create the experiment. An experiment with title \"" + experimentDto.getTitle() + "\" already exists in this course.");
            }
        }
        experimentToChange.setTitle(experimentDto.getTitle());
        experimentToChange.setDescription(experimentDto.getDescription());
        if (experimentToChange.getStarted()!=null
                && (!experimentDto.getExposureType().equals(experimentToChange.getExposureType().name()))){
            throw new WrongValueException("Error 110: The experiment has started. The Exposure Type can't be changed");
        }
        if (experimentToChange.getStarted()!=null
                && (!experimentDto.getDistributionType().equals(experimentToChange.getDistributionType().name()))){
            throw new WrongValueException("Error 110: The experiment has started. The Distribution Type can't be changed");
        }
        if (experimentToChange.getStarted()!=null
                && (!experimentDto.getParticipationType().equals(experimentToChange.getParticipationType().name()))
                && experimentToChange.getParticipationType().equals(ParticipationTypes.CONSENT)){
            throw new WrongValueException("Error 110: The experiment has started. The Participation Type can't be changed from 'Consent' to " + experimentDto.getParticipationType());
        }
        if (experimentDto.getExposureType() != null) {
            if (EnumUtils.isValidEnum(ExposureTypes.class, experimentDto.getExposureType())) {
                experimentToChange.setExposureType(
                        EnumUtils.getEnum(ExposureTypes.class, experimentDto.getExposureType()));
            } else {
                throw new WrongValueException("Error 134: " + experimentDto.getExposureType() + " is not a valid Exposure value");
            }
        }
        if (experimentDto.getDistributionType() != null) {
            if (EnumUtils.isValidEnum(DistributionTypes.class, experimentDto.getDistributionType())) {
                experimentToChange.setDistributionType(
                        EnumUtils.getEnum(DistributionTypes.class, experimentDto.getDistributionType()));
            } else {
                throw new WrongValueException("Error 134: " + experimentDto.getDistributionType() + " is not a valid Distribution value");
            }
        }
        if (experimentDto.getParticipationType() != null) {
            if (EnumUtils.isValidEnum(ParticipationTypes.class, experimentDto.getParticipationType())) {
                experimentToChange.setParticipationType(
                        EnumUtils.getEnum(ParticipationTypes.class, experimentDto.getParticipationType()));
            } else {
                throw new WrongValueException("Error 134: " + experimentDto.getParticipationType() + " is not a valid Participation value");
            }
        }
        experimentToChange.setClosed(experimentDto.getClosed());
        experimentToChange.setStarted(experimentDto.getStarted());
        save(experimentToChange);
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
        if (conditions){
            //TODO, add sort if needed
            List<Condition> conditionList = allRepositories.conditionRepository.findByExperiment_ExperimentId(experiment.getExperimentId());
            for (Condition condition:conditionList){
                ConditionDto conditionDto = conditionService.toDto(condition);
                conditionDtoList.add(conditionDto);
            }
        }
        experimentDto.setConditions(conditionDtoList);

        List<ExposureDto> exposureDtoList = new ArrayList<>();
        if(exposures){
            //TODO add sort if needed
            List<Exposure> exposureList = allRepositories.exposureRepository.findByExperiment_ExperimentId(experiment.getExperimentId());
            for(Exposure exposure : exposureList) {
                ExposureDto exposureDto = exposureService.toDto(exposure);
                exposureDtoList.add(exposureDto);
            }
        }
        experimentDto.setExposures(exposureDtoList);

        List<ParticipantDto> participantDtoList = new ArrayList<>();
        if (participants){
            //TODO, add sort if needed
            List<Participant> participantList = allRepositories.participantRepository.findByExperiment_ExperimentId(experiment.getExperimentId());
            for (Participant participant : participantList){
                ParticipantDto participantDto = participantService.toDto(participant);
                participantDtoList.add(participantDto);
            }
        }
        experimentDto.setParticipants(participantDtoList);

        ConsentDocument consentDocument = experiment.getConsentDocument();
        if (consentDocument !=null){
            Integer countAnswered = 0;
            for (Participant participant:experiment.getParticipants()){
                if (participant.getConsent()!=null){
                    countAnswered = countAnswered + 1;
                }
            }

            ConsentDto consentDto = new ConsentDto();
            consentDto.setConsentDocumentId(consentDocument.getConsentDocumentId());
            consentDto.setFilePointer(consentDocument.getFilePointer());
            consentDto.setTitle(consentDocument.getTitle());
            consentDto.setHtml(consentDocument.getHtml());
            consentDto.setExpectedConsent(experiment.getParticipants().size());
            consentDto.setAnsweredConsentCount(countAnswered);
            experimentDto.setConsent(consentDto);
        }

        return experimentDto;
    }

    @Override
    public Experiment fromDto(ExperimentDto experimentDto) throws DataServiceException { //TODO add booleans to add extra elements

        //TEST and check if nulls behave correctly
        Experiment experiment = new Experiment();
        experiment.setExperimentId(experimentDto.getExperimentId());
        Optional<LtiContextEntity> ltiContextEntity = allRepositories.contexts.findById(experimentDto.getContextId());
        if (ltiContextEntity.isPresent()) {
            experiment.setLtiContextEntity(ltiContextEntity.get());
        } else {
            throw new DataServiceException("The course defined in the experiment dto does not exist");
        }
        Optional<PlatformDeployment> platformDeployment = allRepositories.platformDeploymentRepository.findById(experimentDto.getPlatformDeploymentId());
        if (platformDeployment.isPresent()) {
            experiment.setPlatformDeployment(platformDeployment.get());
        }else {
            throw new DataServiceException("The platform deployment defined in the experiment dto does not exist");
        }
        experiment.setTitle(experimentDto.getTitle());
        experiment.setDescription(experimentDto.getDescription());
        experiment.setExposureType(EnumUtils.getEnum(ExposureTypes.class, experimentDto.getExposureType(), ExposureTypes.NOSET));
        experiment.setParticipationType(EnumUtils.getEnum(ParticipationTypes.class, experimentDto.getParticipationType(), ParticipationTypes.NOSET));
        experiment.setDistributionType(EnumUtils.getEnum(DistributionTypes.class, experimentDto.getDistributionType(), DistributionTypes.NOSET));
        experiment.setStarted(experimentDto.getStarted());
        experiment.setClosed(experimentDto.getClosed());
        LtiUserEntity user = allRepositories.users.findByUserIdAndPlatformDeployment_KeyId(experimentDto.getCreatedBy(),platformDeployment.get().getKeyId());
        if (user!=null){
            experiment.setCreatedBy(user);
        } else {
            throw new DataServiceException("The user specified to create the experiment does not exist or does not belong to this course");
        }

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
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.experimentRepository.deleteByExperimentId(id);
    }

    @Override
    public ExperimentDto fillContextInfo(ExperimentDto experimentDto, SecuredInfo securedInfo) {
        //Doing this ALWAYS for security reasons. In this way we will never edit or create experiments on other context
        //than the one in the token.
        experimentDto.setContextId(securedInfo.getContextId());
        experimentDto.setPlatformDeploymentId(securedInfo.getPlatformDeploymentId());
        LtiUserEntity user = allRepositories.users.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        if (user!=null){
            experimentDto.setCreatedBy(user.getUserId());
        }
        return experimentDto;
    }

    @Override
    public boolean experimentBelongsToDeploymentAndCourse(Long experimentId, Long platformDeploymentId, Long contextId){
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
        if (!StringUtils.isBlank(experimentDto.getTitle())){
            return null;
        }
        List<Experiment> experimentList = allRepositories.experimentRepository.findByPlatformDeployment_KeyIdAndLtiContextEntity_ContextIdAndCreatedBy_UserKey(securedInfo.getPlatformDeploymentId(), securedInfo.getContextId(), securedInfo.getUserId());
        for (Experiment experiment:experimentList){
            if (StringUtils.isBlank(experiment.getTitle())) {
                return toDto(experiment, false, false, false);
            }
        }
        return null;
    }

    @Override
    public boolean experimentStarted(Experiment experiment){
        // TODO add the condition to consider the experiment started
        return false;
    }

    @Override
    public boolean titleAlreadyExists(String title, Long contextId, Long experimentId){
        return allRepositories.experimentRepository.existsByTitleAndLtiContextEntity_ContextIdAndExperimentIdIsNot(title, contextId, experimentId);
    }
    @Override
    public void copyDto(ExperimentDto existingEmpty, ExperimentDto experimentDto){
        existingEmpty.setDescription(experimentDto.getDescription());
        existingEmpty.setDistributionType(experimentDto.getDistributionType());
        existingEmpty.setParticipationType(experimentDto.getParticipationType());
        existingEmpty.setExposureType(experimentDto.getExposureType());
        existingEmpty.setTitle(experimentDto.getTitle());
        existingEmpty.setStarted(experimentDto.getStarted());
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiment/{id}").buildAndExpand(experimentId).toUri());
        return headers;
    }

    @Override
    public void validateTitle(String title, long contextId) throws TitleValidationException {
        if(!StringUtils.isBlank(title)){
            if(title.length() > 255){
                throw new TitleValidationException("Error 101: Experiment title must be 255 characters or less.");
            }
            if(titleAlreadyExists(title, contextId, 0L)){
                throw new TitleValidationException("Error 102: Unable to create the experiment. An experiment with title \"" + title + "\" already exists in this course.");
            }
        }
    }
}