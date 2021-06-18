package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface OutcomeService {

    List<Outcome> findAllByExposureId(Long exposureId);

    OutcomeDto toDto(Outcome outcome, boolean outcomeScores);

    Outcome fromDto(OutcomeDto outcomeDto) throws DataServiceException;

    Outcome save(Outcome outcome);

    Optional<Outcome> findById(Long id);

    void saveAndFlush(Outcome outcomeToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean outcomeBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long outcomeId);

    List<OutcomePotentialDto> potentialOutcomes(Long experimentId) throws DataServiceException, CanvasApiException;

    void updateOutcomeGrades(Long outcomeId, SecurityInfo securityInfo) throws CanvasApiException, IOException, ParticipantNotUpdatedException;
}
