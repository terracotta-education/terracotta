package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.ExperimentDto;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

public interface ExperimentService {

    List<ExperimentDto> getExperiments(SecuredInfo securedInfo, boolean syncWithLms) throws ConnectionException, TerracottaConnectorException;
    Experiment getExperiment(long experimentId);
    ExperimentDto postExperiment(ExperimentDto experimentDto, SecuredInfo securedInfo) throws DataServiceException, TitleValidationException;
    void updateExperiment(long experimentId, long contextId, ExperimentDto experimentDto, SecuredInfo securedInfo) throws TitleValidationException, WrongValueException, ParticipantNotUpdatedException, ExperimentNotMatchingException, IOException, NumberFormatException, TerracottaConnectorException;
    ExperimentDto toDto(Experiment experiment, boolean conditions, boolean exposures, boolean participants, SecuredInfo securedInfo);
    Experiment fromDto(ExperimentDto experimentDto) throws DataServiceException;
    void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException, IOException, TerracottaConnectorException;
    ExperimentDto fillContextInfo(ExperimentDto experimentDto, SecuredInfo securedInfo);
    void deleteConsentDocument(ConsentDocument consentDocument);
    ExperimentDto getEmptyExperiment(SecuredInfo securedInfo, ExperimentDto experimentDto);
    void copyDto(ExperimentDto existingEmpty, ExperimentDto experimentDto);
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId);
    void validateTitle(String title, long contextId) throws TitleValidationException;

}
