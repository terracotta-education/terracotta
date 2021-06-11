package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.OutcomeScore;
import edu.iu.terracotta.model.app.dto.OutcomeScoreDto;

import java.util.List;
import java.util.Optional;

public interface OutcomeScoreService {

    List<OutcomeScore> findAllByOutcomeId(Long outcomeId);

    OutcomeScoreDto toDto(OutcomeScore outcomeScore);

    OutcomeScore fromDto(OutcomeScoreDto outcomeScoreDto) throws DataServiceException;

    OutcomeScore save(OutcomeScore outcomeScore);

    Optional<OutcomeScore> findById(Long id);

    void saveAndFlush(OutcomeScore outcomeScoreToChange);

    void deleteById(Long id);

    boolean outcomeScoreBelongsToOutcome(Long outcomeId, Long outcomeScoreId);


}
