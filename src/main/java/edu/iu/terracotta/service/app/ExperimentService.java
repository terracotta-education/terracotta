package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.dto.ExperimentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface ExperimentService {
    List<Experiment> findAllByDeploymentIdAndCourseId(long deploymentId, long contextId);

    Optional<Experiment> findOneByDeploymentIdAndCourseIdAndExperimentId(long deploymentId, long contextId, long id);

    ExperimentDto toDto(Experiment experiment, boolean conditions, boolean exposures, boolean participants);

    Experiment fromDto(ExperimentDto experimentDto) throws DataServiceException;

    Experiment save(Experiment experiment);

    Optional<Experiment> findById(Long id);

    void saveAndFlush(Experiment experimentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    ExperimentDto fillContextInfo(ExperimentDto experimentDto, SecuredInfo securedInfo);

    boolean experimentBelongsToDeploymentAndCourse(Long experimentId, Long platformDeploymentId, Long contextId);

    ConsentDocument saveConsentDocument(ConsentDocument consentDocument);

    void deleteConsentDocument(ConsentDocument consentDocument);

    ExperimentDto getEmptyExperiment(SecuredInfo securedInfo, ExperimentDto experimentDto);

    boolean experimentStarted(Experiment experiment);

    boolean titleAlreadyExists(String title, Long contextId, Long ExperimentId);

    void copyDto(ExperimentDto existingEmpty, ExperimentDto experimentDto);
}
