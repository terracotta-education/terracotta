package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.OutcomeScoreService;
import edu.iu.terracotta.service.app.OutcomeService;
import edu.iu.terracotta.service.app.ParticipantService;
import edu.iu.terracotta.utils.TextConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

@Component
public class OutcomeScoreServiceImpl implements OutcomeScoreService {

    @Autowired
    private AllRepositories allRepositories;

    @Autowired
    private OutcomeService outcomeService;

    @Autowired
    private ParticipantService participantService;

    @Override
    public List<OutcomeScore> findAllByOutcomeId(Long outcomeId) {
        return allRepositories.outcomeScoreRepository.findByOutcome_OutcomeId(outcomeId);
    }

    @Override
    public List<OutcomeScoreDto> getOutcomeScores(Long outcomeId) {
        return CollectionUtils.emptyIfNull(findAllByOutcomeId(outcomeId)).stream()
            .map(outcomeScore -> toDto(outcomeScore))
            .toList();
    }

    @Override
    public OutcomeScore getOutcomeScore(Long id) {
        return allRepositories.outcomeScoreRepository.findByOutcomeScoreId(id);
    }

    @Override
    public OutcomeScoreDto postOutcomeScore(OutcomeScoreDto outcomeScoreDto, long experimentId, long outcomeId) throws IdInPostException, InvalidParticipantException, DataServiceException {
        if (outcomeScoreDto.getOutcomeScoreId() != null) {
            throw new IdInPostException(TextConstants.ID_IN_POST_ERROR);
        }

        validateParticipant(outcomeScoreDto.getParticipantId(), experimentId);
        outcomeScoreDto.setOutcomeId(outcomeId);
        OutcomeScore outcomeScore;

        try{
            outcomeScore = fromDto(outcomeScoreDto);
        } catch (DataServiceException ex) {
            throw new DataServiceException("Error 105: Unable to create outcome score: " + ex.getMessage(), ex);
        }

        return toDto(save(outcomeScore));
    }

    @Override
    public OutcomeScoreDto toDto(OutcomeScore outcomeScore) {
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

        if (!outcome.isPresent()) {
            throw new DataServiceException("The outcome for the outcome score does not exist.");
        }

        outcomeScore.setOutcome(outcome.get());

        Optional<Participant> participant = participantService.findById(outcomeScoreDto.getParticipantId());

        if (!participant.isPresent()) {
            throw new DataServiceException("The participant for the outcome score does not exist.");
        }

        outcomeScore.setParticipant(participant.get());

        return outcomeScore;
    }

    @Override
    public OutcomeScore save(OutcomeScore outcomeScore) {
        return allRepositories.outcomeScoreRepository.save(outcomeScore);
    }

    @Override
    public Optional<OutcomeScore> findById(Long id) {
        return allRepositories.outcomeScoreRepository.findById(id);
    }

    @Override
    public void updateOutcomeScore(Long outcomeScoreId, OutcomeScoreDto outcomeScoreDto) {
        OutcomeScore outcomeScore = getOutcomeScore(outcomeScoreId);
        outcomeScore.setScoreNumeric(outcomeScoreDto.getScoreNumeric());
        saveAndFlush(outcomeScore);
    }

    @Override
    public void saveAndFlush(OutcomeScore outcomeScoreToChange) {
        allRepositories.outcomeScoreRepository.saveAndFlush(outcomeScoreToChange);
    }

    @Override
    public void deleteById(Long id) {
        allRepositories.outcomeScoreRepository.deleteByOutcomeScoreId(id);
    }

    @Override
    public boolean outcomeScoreBelongsToOutcome(Long outcomeId, Long outcomeScoreId) {
        return allRepositories.outcomeScoreRepository.existsByOutcome_OutcomeIdAndOutcomeScoreId(outcomeId, outcomeScoreId);
    }

    @Override
    public void validateParticipant(Long participantId, Long experimentId) throws InvalidParticipantException {
        if (participantId == null) {
            throw new InvalidParticipantException("Error 105: Must include a valid participant id in the POST");
        }

        Optional<Participant> participant = participantService.findByParticipantIdAndExperimentId(participantId, experimentId);

        if (!participant.isPresent()) {
            throw new InvalidParticipantException("Error 109: The participant provided does not belong to this experiment.");
        }
    }

    @Override
    public HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId, Long outcomeId, Long outcomeScoreId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/api/experiments/{experimentId}/exposures/{exposureId}/outcomes/{outcomeId}/outcome_scores/{outcomeScoreId}")
                .buildAndExpand(experimentId, exposureId, outcomeId, outcomeScoreId).toUri());

        return headers;
    }

}