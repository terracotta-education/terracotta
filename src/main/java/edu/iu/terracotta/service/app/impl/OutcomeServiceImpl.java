package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Exposure;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import edu.iu.terracotta.model.app.enumerator.LmsType;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.ExposureService;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.service.app.OutcomeService;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class OutcomeServiceImpl implements OutcomeService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    OutcomeScoreService outcomeScoreService;

    @Autowired
    ExposureService exposureService;

    @Override
    public List<Outcome> findAllByExposureId(Long exposureId) {
        return allRepositories.outcomeRepository.findByExposure_ExposureId(exposureId);
    }

    @Override
    public OutcomeDto toDto(Outcome outcome, boolean outcomeScores) {
        OutcomeDto outcomeDto = new OutcomeDto();
        outcomeDto.setOutcomeId(outcome.getOutcomeId());
        outcomeDto.setExposureId(outcome.getExposure().getExposureId());
        outcomeDto.setTitle(outcome.getTitle());
        outcomeDto.setLmsType(outcome.getLmsType().name());
        outcomeDto.setLmsOutcomeId(outcome.getLmsOutcomeId());
        outcomeDto.setMaxPoints(outcome.getMaxPoints());
        outcomeDto.setExternal(outcome.getExternal());
        List<OutcomeScoreDto> outcomeScoreDtoList = new ArrayList<>();
        if(outcomeScores) {
            List<OutcomeScore> outcomeScoreList = allRepositories.outcomeScoreRepository.findByOutcome_OutcomeId(outcome.getOutcomeId());
            for(OutcomeScore outcomeScore : outcomeScoreList) {
                outcomeScoreDtoList.add(outcomeScoreService.toDto(outcomeScore));
            }
        }
        outcomeDto.setOutcomeScoreDtoList(outcomeScoreDtoList);

        return outcomeDto;
    }

    @Override
    public Outcome fromDto(OutcomeDto outcomeDto) throws DataServiceException{
        Outcome outcome = new Outcome();
        outcome.setOutcomeId(outcomeDto.getOutcomeId());
        outcome.setTitle(outcomeDto.getTitle());
        outcome.setLmsType(EnumUtils.getEnum(LmsType.class, outcomeDto.getLmsType(), LmsType.NONE));
        outcome.setMaxPoints(outcomeDto.getMaxPoints());
        outcome.setLmsOutcomeId(outcomeDto.getLmsOutcomeId());
        outcome.setExternal(outcomeDto.getExternal());
        Optional<Exposure> exposure = exposureService.findById(outcomeDto.getExposureId());
        if(exposure.isPresent()){
            outcome.setExposure(exposure.get());
        } else{
            throw new DataServiceException("Exposure for outcome does not exist.");
        }

        return outcome;
    }

    @Override
    public Outcome save(Outcome outcome) { return allRepositories.outcomeRepository.save(outcome); }

    @Override
    public Optional<Outcome> findById(Long id) { return allRepositories.outcomeRepository.findById(id); }

    @Override
    public void saveAndFlush(Outcome outcomeToChange) { allRepositories.outcomeRepository.saveAndFlush(outcomeToChange); }

    @Override
    public void deleteById(Long id) { allRepositories.outcomeRepository.deleteById(id); }

    @Override
    public boolean outcomeBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long outcomeId){
        return allRepositories.outcomeRepository.existsByExposure_Experiment_ExperimentIdAndExposure_ExposureIdAndOutcomeId(experimentId, exposureId, outcomeId);
    }

}
