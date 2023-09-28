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

public interface OutcomeService {

    List<OutcomeDto> getOutcomesForExposure(long exposureId);

    Outcome getOutcome(long id);

    OutcomeDto postOutcome(OutcomeDto outcomeDto, long exposureId) throws IdInPostException, DataServiceException, TitleValidationException;

    OutcomeDto toDto(Outcome outcome, boolean outcomeScores);

    Outcome fromDto(OutcomeDto outcomeDto) throws DataServiceException;

    void updateOutcome(long outcomeId, OutcomeDto outcomeDto) throws TitleValidationException;

    void deleteById(long id) throws EmptyResultDataAccessException;

    List<OutcomePotentialDto> potentialOutcomes(long experimentId, SecuredInfo securedInfo) throws DataServiceException, CanvasApiException;

    void updateOutcomeGrades(long outcomeId, SecuredInfo securedInfo) throws CanvasApiException, IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException;

    void defaultOutcome(OutcomeDto outcomeDto) throws TitleValidationException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long outcomeId);

}
