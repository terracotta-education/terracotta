package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.model.app.dto.GroupConditionDto;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ExposureServiceImpl implements ExposureService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    ExperimentService experimentService;

    @Override
    public List<Exposure> findAllByExperimentId(long experimentId) {
        return allRepositories.exposureRepository.findByExperiment_ExperimentId(experimentId);
    }

    @Override
    public List<ExposureDto> getExposures(Long experimentId){
        List<Exposure> exposures = findAllByExperimentId(experimentId);
        List<ExposureDto> exposureDtoList = new ArrayList<>();
        for(Exposure exposure : exposures){
            exposureDtoList.add(toDto(exposure));
        }
        return exposureDtoList;
    }

    @Override
    public ExposureDto postExposure(ExposureDto exposureDto, long experimentId) throws IdInPostException, DataServiceException, TitleValidationException{
        if(exposureDto.getExposureId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        validateTitle(exposureDto.getTitle());
        exposureDto.setExperimentId(experimentId);
        Exposure exposure;
        try{
            exposure = fromDto(exposureDto);
        } catch (DataServiceException e) {
            throw new DataServiceException("Error 105: Unable to create exposure:" + e.getMessage());
        }
        return toDto(save(exposure));
    }

    @Override
    public ExposureDto toDto(Exposure exposure) {

        ExposureDto exposureDto = new ExposureDto();
        exposureDto.setExposureId(exposure.getExposureId());
        exposureDto.setExperimentId(exposure.getExperiment().getExperimentId());
        exposureDto.setTitle(exposure.getTitle());
        List<ExposureGroupCondition> exposureGroupConditionList = allRepositories.exposureGroupConditionRepository.findByExposure_ExposureId(exposure.getExposureId());
        List<GroupConditionDto> conditionDtoGroupDtoList = new ArrayList<>();
        for (ExposureGroupCondition exposureGroupCondition:exposureGroupConditionList){
            Group group = exposureGroupCondition.getGroup();
            Condition condition = exposureGroupCondition.getCondition();
            GroupConditionDto groupConditionDto = new GroupConditionDto();
            groupConditionDto.setConditionId(condition.getConditionId());
            groupConditionDto.setConditionName(condition.getName());
            groupConditionDto.setGroupId(group.getGroupId());
            groupConditionDto.setGroupName(group.getName());
            conditionDtoGroupDtoList.add(groupConditionDto);
        }
        exposureDto.setGroupConditionList(conditionDtoGroupDtoList);
        return exposureDto;
    }

    @Override
    public Exposure fromDto(ExposureDto exposureDto) throws DataServiceException {

        Exposure exposure = new Exposure();
        exposure.setExposureId(exposureDto.getExposureId());
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(exposureDto.getExperimentId());
        if(experiment.isPresent()) {
            exposure.setExperiment(experiment.get());
        } else {
            throw new DataServiceException("The experiment for the exposure does not exist");
        }

        exposure.setTitle(exposureDto.getTitle());
        return exposure;
    }

    @Override
    @Transactional
    public void createExposures(Long experimentId) throws DataServiceException, ExperimentStartedException {
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(experimentId);

        if (!experiment.isPresent()) {
            throw new DataServiceException("The experiment for the exposure does not exist");
        }

        int exposuresToCreate = 1;

        if (ExposureTypes.WITHIN.equals(experiment.get().getExposureType())) {
            exposuresToCreate = experiment.get().getConditions().size();
        }

        List<Exposure> exposures = experiment.get().getExposures();

        if (CollectionUtils.isNotEmpty(exposures)) {
            if (exposures.size() == exposuresToCreate) {
                return;
            }

            if (experimentService.experimentStarted(experiment.get())){
                throw new ExperimentStartedException("Error 110: The experiment has already started. We can't modify it");
            } else{
                // delete the existing exposures. That means delete the exposureGroupConditions too.
                allRepositories.exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
                allRepositories.exposureRepository.deleteByExperiment_ExperimentId(experimentId);
            }
        }

        for (int order = 1; order <= exposuresToCreate; order++) {
            Exposure exposure = new Exposure();
            exposure.setExperiment(experiment.get());
            exposure.setTitle("Exposure " + order);
            save(exposure);
        }
    }

    @Override
    public Exposure save(Exposure exposure) { return allRepositories.exposureRepository.save(exposure); }

    @Override
    public Optional<Exposure> findById(Long id) { return allRepositories.exposureRepository.findById(id); }

    @Override
    public Exposure getExposure(Long id){ return allRepositories.exposureRepository.findByExposureId(id); }

    @Override
    public void updateExposure(Long exposureId, ExposureDto exposureDto) throws TitleValidationException{
        Exposure exposure = allRepositories.exposureRepository.findByExposureId(exposureId);
        if(StringUtils.isAllBlank(exposureDto.getTitle()) && StringUtils.isAllBlank(exposure.getTitle())){
            throw new TitleValidationException("Error 100: Please give the exposure a title.");
        }
        if(!StringUtils.isAllBlank(exposureDto.getTitle()) && exposureDto.getTitle().length() > 255) {
            throw new TitleValidationException("Error 101: Title must be 255 characters or less.");
        }
        exposure.setTitle(exposureDto.getTitle());
        saveAndFlush(exposure);
    }

    @Override
    public void saveAndFlush(Exposure exposureToChange) { allRepositories.exposureRepository.saveAndFlush(exposureToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.exposureRepository.deleteByExposureId(id); }

    @Override
    public boolean exposureBelongsToExperiment(Long experimentId, Long exposureId) {
        return allRepositories.exposureRepository.existsByExperiment_ExperimentIdAndExposureId(experimentId,exposureId);
    }

    @Override
    public void validateTitle(String title) throws TitleValidationException{
        if(!StringUtils.isAllBlank(title) && title.length() > 255){
            throw new TitleValidationException("Title must be 255 characters or less.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/exposures/{id}").buildAndExpand(experimentId, exposureId).toUri());
        return headers;
    }
}