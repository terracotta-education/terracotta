package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface TreatmentService {

    List<Treatment> findAllByConditionId(Long conditionId);

    TreatmentDto toDto(Treatment treatment, boolean submissions) throws AssessmentNotMatchingException;

    Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException;

    Treatment save(Treatment treatment);

    Optional<Treatment> findById(Long id);

    void saveAndFlush(Treatment treatmentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean treatmentBelongsToExperimentAndCondition(Long experimentId, Long conditionId, Long treatmentId);
}
