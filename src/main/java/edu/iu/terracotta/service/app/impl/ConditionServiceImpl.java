package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.service.app.ConditionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ConditionServiceImpl implements ConditionService {

    @Autowired
    AllRepositories allRepositories;

    @Override
    public List<ConditionDto> findAllByExperimentId(long experimentId) {
        List<Condition> conditions = allRepositories.conditionRepository.findByExperiment_ExperimentId(experimentId);
        List<ConditionDto> conditionDtoList = new ArrayList<>();
        if(!conditions.isEmpty()){
            for(Condition condition : conditions) {
                conditionDtoList.add(toDto(condition));
            }
        }
        return conditionDtoList;
    }

    @Override
    public ConditionDto toDto(Condition condition) {

        ConditionDto conditionDto = new ConditionDto();
        conditionDto.setConditionId(condition.getConditionId());
        conditionDto.setExperimentId(condition.getExperiment().getExperimentId());
        conditionDto.setName(condition.getName());
        conditionDto.setDefaultCondition(condition.getDefaultCondition());
        conditionDto.setDistributionPct(condition.getDistributionPct());

        return conditionDto;
    }

    @Override
    public Condition fromDto(ConditionDto conditionDto) throws DataServiceException {

        Condition condition = new Condition();
        condition.setConditionId(conditionDto.getConditionId());
        Optional<Experiment> experiment = allRepositories.experimentRepository.findById(conditionDto.getExperimentId());
        if (experiment.isPresent()){
            condition.setExperiment(experiment.get());
        } else {
            throw new DataServiceException("The experiment for the condition does not exist");
        }

        condition.setName(conditionDto.getName());
        condition.setDefaultCondition(conditionDto.getDefaultCondition());
        condition.setDistributionPct(conditionDto.getDistributionPct());

        return condition;
    }

    @Override
    public Condition save(Condition condition) {
        return allRepositories.conditionRepository.save(condition);
    }

    @Override
    public Optional<Condition> findById(Long id) { return allRepositories.conditionRepository.findById(id); }

    @Override
    public Condition findByConditionId(Long conditionId) { return allRepositories.conditionRepository.findByConditionId(conditionId); }

    @Override
    public ConditionDto getCondition(Long id) {
        return toDto(findByConditionId(id));
    }

    @Override
    public void saveAndFlush(Condition conditionToChange) { allRepositories.conditionRepository.saveAndFlush(conditionToChange); }

    @Override
    @Transactional
    public void updateCondition(Map<Condition, ConditionDto> map){
        for(Map.Entry<Condition, ConditionDto> entry : map.entrySet()){
            Condition condition = entry.getKey();
            ConditionDto conditionDto = entry.getValue();
            condition.setName(conditionDto.getName());
            condition.setDefaultCondition(conditionDto.getDefaultCondition());
            condition.setDistributionPct((conditionDto.getDistributionPct()));
            save(condition);
        }
    }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.conditionRepository.deleteByConditionId(id);
    }

    @Override
    public boolean conditionBelongsToExperiment(Long experimentId, Long conditionId) {
        return allRepositories.conditionRepository.existsByExperiment_ExperimentIdAndConditionId(experimentId,conditionId);
    }

    @Override
    public boolean nameAlreadyExists(String name, Long experimentId, Long conditionId){
        return allRepositories.conditionRepository.existsByNameAndExperiment_ExperimentIdAndConditionIdIsNot(name, experimentId, conditionId);
    }

    @Override
    public boolean duplicateNameInPut(Map<Condition, ConditionDto> map, Condition condition){
        for(Map.Entry<Condition, ConditionDto> entry : map.entrySet()){
            Condition conditionInPut = entry.getKey();
            if(condition.getName().equals(conditionInPut.getName())){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDefaultCondition(Long conditionId){
        return allRepositories.conditionRepository.existsByConditionIdAndDefaultCondition(conditionId, true);
    }

    @Override
    public HttpHeaders buildHeader(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId){
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experiment_id}/conditions/{condition_id}")
                .buildAndExpand(experimentId, conditionId).toUri());
        return headers;
    }

    @Override
    public void validateConditionName(String conditionName, String dtoName, Long experimentId, Long conditionId, boolean required) throws TitleValidationException{
        if(required){
            if(StringUtils.isAllBlank(dtoName) && StringUtils.isAllBlank(conditionName)){
                throw new TitleValidationException("Error 100: Please give the condition a name.");
            }
        }
        if(!StringUtils.isBlank(dtoName)){
            if(dtoName.length() > 255){
                throw new TitleValidationException("Error 101: Condition name must be 255 characters or less.");
            }
            if(nameAlreadyExists(dtoName, experimentId, conditionId)){
                throw new TitleValidationException("Error 102: Unable to create the condition. A condition with title \"" + dtoName + "\" already exists in this experiment.");
            }
        }
    }
}
