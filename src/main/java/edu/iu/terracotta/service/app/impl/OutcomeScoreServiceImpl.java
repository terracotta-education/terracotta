package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.ParticipantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OutcomeScoreServiceImpl implements OutcomeScoreService {

    @Autowired
    AllRepositories allRepositories;

    @Autowired
    OutcomeService outcomeService;

    @Autowired
    ParticipantService participantService;

    @Override
    public List<OutcomeScore> findAllByOutcomeId(Long outcomeId) { return allRepositories.outcomeScoreRepository.findByOutcome_OutcomeId(outcomeId);}

    @Override
    public OutcomeScoreDto toDto(OutcomeScore outcomeScore){
        OutcomeScoreDto outcomeScoreDto = new OutcomeScoreDto();
        outcomeScoreDto.setOutcomeScoreId(outcomeScore.getOutcomeScoreId());
        outcomeScoreDto.setOutcomeId(outcomeScore.getOutcome().getOutcomeId());
        outcomeScoreDto.setParticipantId(outcomeScore.getParticipant().getParticipantId());
        outcomeScoreDto.setScoreNumeric(outcomeScore.getScoreNumeric());

        return outcomeScoreDto;
    }

    @Override
    public OutcomeScore fromDto(OutcomeScoreDto outcomeScoreDto) throws DataServiceException {
        OutcomeScore outcomeScore = new OutcomeScore();
        outcomeScore.setOutcomeScoreId(outcomeScoreDto.getOutcomeScoreId());
        outcomeScore.setScoreNumeric(outcomeScoreDto.getScoreNumeric());
        Optional<Outcome> outcome =  outcomeService.findById(outcomeScoreDto.getOutcomeId());
        if(outcome.isPresent()){
            outcomeScore.setOutcome(outcome.get());
        } else {
            throw new DataServiceException("The outcome for the outcome score does not exist.");
        }
        Optional<Participant> participant = participantService.findById(outcomeScoreDto.getParticipantId());
        if(participant.isPresent()){
            outcomeScore.setParticipant(participant.get());
        } else {
            throw new DataServiceException("The participant for the outcome score does not exist.");
        }
        return outcomeScore;
    }

    @Override
    public OutcomeScore save(OutcomeScore outcomeScore) { return allRepositories.outcomeScoreRepository.save(outcomeScore); }

    @Override
    public Optional<OutcomeScore> findById(Long id) { return allRepositories.outcomeScoreRepository.findById(id); }

    @Override
    public void saveAndFlush(OutcomeScore outcomeScoreToChange) { allRepositories.outcomeScoreRepository.saveAndFlush(outcomeScoreToChange); }

    @Override
    public void deleteById(Long id) { allRepositories.outcomeScoreRepository.deleteByOutcomeScoreId(id); }

    @Override
    public boolean outcomeScoreBelongsToOutcome(Long outcomeId, Long outcomeScoreId) {
        return allRepositories.outcomeScoreRepository.existsByOutcome_OutcomeIdAndOutcomeScoreId(outcomeId, outcomeScoreId);
    }
}
