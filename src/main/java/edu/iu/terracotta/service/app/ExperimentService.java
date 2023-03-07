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
import java.util.Optional;

public interface ExperimentService {
    List<Experiment> findAllByDeploymentIdAndCourseId(long deploymentId, long contextId);

    List<ExperimentDto> getExperiments(SecuredInfo securedInfo, boolean syncWithCanvas);

    Experiment getExperiment(long experimentId);

    ExperimentDto postExperiment(ExperimentDto experimentDto, SecuredInfo securedInfo) throws DataServiceException, TitleValidationException;

    void updateExperiment(long experimentId, long contextId, ExperimentDto experimentDto, SecuredInfo securedInfo) throws TitleValidationException, WrongValueException, ParticipantNotUpdatedException, ExperimentNotMatchingException;

    Optional<Experiment> findOneByDeploymentIdAndCourseIdAndExperimentId(long deploymentId, long contextId, long id);

    ExperimentDto toDto(Experiment experiment, boolean conditions, boolean exposures, boolean participants);

    Experiment fromDto(ExperimentDto experimentDto) throws DataServiceException;

    Experiment save(Experiment experiment);

    Optional<Experiment> findById(Long id);

    void saveAndFlush(Experiment experimentToChange);

    void deleteById(Long id, SecuredInfo securedInfo) throws EmptyResultDataAccessException;

    ExperimentDto fillContextInfo(ExperimentDto experimentDto, SecuredInfo securedInfo);

    boolean experimentBelongsToDeploymentAndCourse(Long experimentId, Long platformDeploymentId, Long contextId);

    ConsentDocument saveConsentDocument(ConsentDocument consentDocument);

    void deleteConsentDocument(ConsentDocument consentDocument);

    ExperimentDto getEmptyExperiment(SecuredInfo securedInfo, ExperimentDto experimentDto);

    boolean titleAlreadyExists(String title, Long contextId, Long experimentId);

    void copyDto(ExperimentDto existingEmpty, ExperimentDto experimentDto);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId);

    void validateTitle(String title, long contextId) throws TitleValidationException;
}
