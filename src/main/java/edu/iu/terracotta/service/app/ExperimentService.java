package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.WrongValueException;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.ExperimentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

public interface ExperimentService {

    List<ExperimentDto> getExperiments(SecuredInfo securedInfo, boolean syncWithCanvas);
    Experiment getExperiment(long experimentId);
    ExperimentDto postExperiment(ExperimentDto experimentDto, SecuredInfo securedInfo) throws DataServiceException, TitleValidationException;
    void updateExperiment(long experimentId, long contextId, ExperimentDto experimentDto, SecuredInfo securedInfo) throws TitleValidationException, WrongValueException, ParticipantNotUpdatedException, ExperimentNotMatchingException;
    ExperimentDto toDto(Experiment experiment, boolean conditions, boolean exposures, boolean participants);
    Experiment fromDto(ExperimentDto experimentDto) throws DataServiceException;
    void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException;
    ExperimentDto fillContextInfo(ExperimentDto experimentDto, SecuredInfo securedInfo);
    void deleteConsentDocument(ConsentDocument consentDocument);
    ExperimentDto getEmptyExperiment(SecuredInfo securedInfo, ExperimentDto experimentDto);
    void copyDto(ExperimentDto existingEmpty, ExperimentDto experimentDto);
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId);
    void validateTitle(String title, long contextId) throws TitleValidationException;

}
