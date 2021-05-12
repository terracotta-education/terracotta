package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.ConditionDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.model.app.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ConditionService {

    @Autowired
    AllRepositories allRepositories;

    public List<Condition> findAllByExperimentId(long experimentId) {
        return allRepositories.conditionRepository.findByExperiment_ExperimentId(experimentId);
    }

    public Optional<Condition> findOneByConditionId(long conditionId) {
        return allRepositories.conditionRepository.findByConditionId(conditionId);
    }

    public ConditionDto toDto(Condition condition) {

        ConditionDto conditionDto = new ConditionDto();
        conditionDto.setConditionId(condition.getConditionId());
        //TODO check null??
        conditionDto.setExperimentId(condition.getExperiment().getExperimentId());
        conditionDto.setName(condition.getName());
        conditionDto.setDefaultCondition(condition.getDefaultCondition());
        conditionDto.setDistributionPct(condition.getDistributionPct());

        return conditionDto;
    }

    public Condition fromDto(ConditionDto conditionDto) throws DataServiceException {

        Condition condition = new Condition();
        //Test if nulls behave correctly??

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

    public Condition save(Condition condition) {
        return allRepositories.conditionRepository.save(condition);
    }

    //needed??
    public Optional<Condition> findById(Long id) { return allRepositories.conditionRepository.findById(id); }

    public void saveAndFlush(Condition conditionToChange) {
        allRepositories.conditionRepository.saveAndFlush(conditionToChange);
    }

    public void deleteById(Long id) throws EmptyResultDataAccessException {
        allRepositories.conditionRepository.deleteById(id);
    }

}
