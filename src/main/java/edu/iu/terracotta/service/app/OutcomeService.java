package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Outcome;
import edu.iu.terracotta.model.app.dto.OutcomeDto;
import edu.iu.terracotta.model.app.dto.OutcomePotentialDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface OutcomeService {

    List<Outcome> findAllByExposureId(Long exposureId);

    List<Outcome> findAllByExperiment(long experimentId);

    List<OutcomeDto> getOutcomes(Long exposureId);

    Outcome getOutcome(Long id);

    OutcomeDto postOutcome(OutcomeDto outcomeDto, long exposureId) throws IdInPostException, DataServiceException, TitleValidationException;

    OutcomeDto toDto(Outcome outcome, boolean outcomeScores);

    Outcome fromDto(OutcomeDto outcomeDto) throws DataServiceException;

    Outcome save(Outcome outcome);

    Optional<Outcome> findById(Long id);

    void updateOutcome(Long outcomeId, OutcomeDto outcomeDto) throws TitleValidationException;

    void saveAndFlush(Outcome outcomeToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean outcomeBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long outcomeId);

    List<OutcomePotentialDto> potentialOutcomes(Long experimentId) throws DataServiceException, CanvasApiException;

    void updateOutcomeGrades(Long outcomeId, SecuredInfo securedInfo) throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException;

    void defaultOutcome(OutcomeDto outcomeDto) throws TitleValidationException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long exposureId, Long outcomeId);
}
