package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.IdMismatchException;
import edu.iu.terracotta.exceptions.IdMissingException;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface TreatmentService {

    List<Treatment> findAllByConditionId(Long conditionId);

    List<TreatmentDto> getTreatments(Long conditionId, String canvasCourseId, long platformDeploymentId, boolean submissions) throws AssessmentNotMatchingException, NumberFormatException, CanvasApiException;

    Treatment getTreatment(Long id);

    TreatmentDto postTreatment(TreatmentDto treatmentDto, long conditionId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException;

    TreatmentDto putTreatment(TreatmentDto treatmentDto, long treatmentId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, IdMissingException, IdMismatchException;

    TreatmentDto duplicateTreatment(long treatmentId, String canvasCourseId, long platformDeploymentId) throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException, CanvasApiException;

    TreatmentDto toDto(Treatment treatment, boolean submissions, boolean addAssignmentDto) throws AssessmentNotMatchingException;

    Treatment fromDto(TreatmentDto treatmentDto) throws DataServiceException;

    Treatment save(Treatment treatment);

    Optional<Treatment> findById(Long id);

    void saveAndFlush(Treatment treatmentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean treatmentBelongsToExperimentAndCondition(Long experimentId, Long conditionId, Long treatmentId);

    void limitToOne(long assignmentId, long conditionId) throws ExceedingLimitException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId);
}
