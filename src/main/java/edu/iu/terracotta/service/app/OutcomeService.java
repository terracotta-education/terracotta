package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.OutcomeDto;
import edu.iu.terracotta.dao.model.dto.OutcomePotentialDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

public interface OutcomeService {

    List<OutcomeDto> getOutcomesForExposure(long exposureId);
    List<OutcomeDto> getAllByExperiment(long experimentId);
    Outcome getOutcome(long id);
    OutcomeDto postOutcome(OutcomeDto outcomeDto, long exposureId) throws IdInPostException, DataServiceException, TitleValidationException;
    OutcomeDto toDto(Outcome outcome, boolean outcomeScores);
    Outcome fromDto(OutcomeDto outcomeDto) throws DataServiceException;
    void updateOutcome(long outcomeId, OutcomeDto outcomeDto) throws TitleValidationException;
    void deleteById(long id) throws EmptyResultDataAccessException;
    List<OutcomePotentialDto> potentialOutcomes(long experimentId, SecuredInfo securedInfo) throws DataServiceException, ApiException, TerracottaConnectorException;
    void updateOutcomeGrades(long outcomeId, SecuredInfo securedInfo, boolean refreshParticipants) throws IOException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, NumberFormatException, TerracottaConnectorException;
    void defaultOutcome(OutcomeDto outcomeDto) throws TitleValidationException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long exposureId, long outcomeId);

}
