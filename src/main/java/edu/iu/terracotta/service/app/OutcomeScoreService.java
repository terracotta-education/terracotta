package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface OutcomeScoreService {

    List<OutcomeScoreDto> getOutcomeScores(Long outcomeId);

    OutcomeScore getOutcomeScore(Long id);

    OutcomeScoreDto postOutcomeScore(OutcomeScoreDto outcomeScoreDto, long experimentId, long outcomeId) throws IdInPostException, DataServiceException, InvalidParticipantException;

    OutcomeScoreDto toDto(OutcomeScore outcomeScore);

    OutcomeScore fromDto(OutcomeScoreDto outcomeScoreDto) throws DataServiceException;

    void updateOutcomeScore(Long outcomeId, OutcomeScoreDto outcomeScoreDto);

    void deleteById(Long id);

    void validateParticipant(Long participantId, Long experimentId) throws InvalidParticipantException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId, Long outcomeId, Long outcomeScoreId);
}
