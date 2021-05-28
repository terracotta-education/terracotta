package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface AssessmentService {

    List<Assessment> findAllByTreatmentId(Long treatmentId);

    AssessmentDto toDto(Assessment assessment);

    Assessment fromDto(AssessmentDto assessmentDto) throws DataServiceException;

    Assessment save(Assessment assessment);

    Optional<Assessment> findById(Long id);

    void saveAndFlush(Assessment assessmentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean assessmentBelongsToExperimentAndConditionAndTreatment(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);
}
