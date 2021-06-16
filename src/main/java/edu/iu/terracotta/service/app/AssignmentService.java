package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface AssignmentService {

    List<Assignment> findAllByExposureId(long exposureId);

    AssignmentDto toDto(Assignment assignment);

    Assignment fromDto(AssignmentDto assignmentDto) throws DataServiceException;

    Assignment save(Assignment assignment);

    Optional<Assignment> findById(Long id);

    void saveAndFlush(Assignment assignmentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean assignmentBelongsToExperimentAndExposure(Long experimentId, Long exposureId, Long assignmentId);

    boolean assignmentBelongsToExperiment(Long experimentId, Long assignmentId);

    String lineItemId(Assignment assignment) throws ConnectionException;

    void sendAssignmentGradeToCanvas(Assignment assignment) throws ConnectionException, DataServiceException;


}
