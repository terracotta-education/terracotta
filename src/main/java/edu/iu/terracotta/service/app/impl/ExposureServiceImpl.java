package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentStartedException;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.ExposureGroupCondition;
import edu.iu.terracotta.model.app.Group;
import edu.iu.terracotta.model.app.dto.ExposureDto;
import edu.iu.terracotta.model.app.dto.GroupConditionDto;
import edu.iu.terracotta.model.app.enumerator.ExposureTypes;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ConditionService;
import edu.iu.terracotta.service.app.ExperimentService;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        if (experiment.isPresent()) {
            Experiment experiment1 = experiment.get();
            int exposuresToCreate = 0;
            if (experiment1.getExposureType().equals(ExposureTypes.WITHIN)) {
                exposuresToCreate = experiment1.getConditions().size();
            } else if (experiment1.getExposureType().equals(ExposureTypes.BETWEEN)) {
                exposuresToCreate = 1;
            }
            List<Exposure> exposures = experiment1.getExposures();
            if (exposures!=null && !exposures.isEmpty()){
                if (exposures.size()==exposuresToCreate){
                    return;
                } else {
                    if (experimentService.experimentStarted(experiment1)){
                        throw new ExperimentStartedException("The experiment has already started. We can't modify it");
                    } else{
                        //delete the existing exposures. That means delete the exposureGroupConditions too.
                        allRepositories.exposureGroupConditionRepository.deleteByExposure_Experiment_ExperimentId(experimentId);
                        allRepositories.exposureRepository.deleteByExperiment_ExperimentId(experimentId);
                    }
                }
            }
            for (int order = 1; order <= exposuresToCreate; order++) {
                Exposure exposure = new Exposure();
                exposure.setExperiment(experiment.get());
                exposure.setTitle("Exposure " + order);
                save(exposure);
            }
        } else {
            throw new DataServiceException("The experiment for the exposure does not exist");
        }
    }

    @Override
    public Exposure save(Exposure exposure) { return allRepositories.exposureRepository.save(exposure); }

    @Override
    public Optional<Exposure> findById(Long id) { return allRepositories.exposureRepository.findById(id); }

    @Override
    public void saveAndFlush(Exposure exposureToChange) { allRepositories.exposureRepository.saveAndFlush(exposureToChange); }

    @Override
    public void deleteById(Long id) throws EmptyResultDataAccessException { allRepositories.exposureRepository.deleteById(id); }

    @Override
    public boolean exposureBelongsToExperiment(Long experimentId, Long exposureId) {
        return allRepositories.exposureRepository.existsByExperiment_ExperimentIdAndExposureId(experimentId,exposureId);
    }



}
