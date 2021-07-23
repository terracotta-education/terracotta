package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.InvalidParticipantException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface OutcomeScoreService {

    List<OutcomeScore> findAllByOutcomeId(Long outcomeId);

    List<OutcomeScoreDto> getOutcomeScores(Long outcomeId);

    OutcomeScore getOutcomeScore(Long id);

    OutcomeScoreDto toDto(OutcomeScore outcomeScore);

    OutcomeScore fromDto(OutcomeScoreDto outcomeScoreDto) throws DataServiceException;

    OutcomeScore save(OutcomeScore outcomeScore);

    Optional<OutcomeScore> findById(Long id);

    void updateOutcomeScore(Long outcomeId, OutcomeScoreDto outcomeScoreDto);

    void saveAndFlush(OutcomeScore outcomeScoreToChange);

    void deleteById(Long id);

    boolean outcomeScoreBelongsToOutcome(Long outcomeId, Long outcomeScoreId);

    void validateParticipant(Long participantId, Long experimentId) throws InvalidParticipantException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId, Long outcomeId, Long outcomeScoreId);
}
