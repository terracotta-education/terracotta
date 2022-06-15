package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface AssessmentService {

    List<Assessment> findAllByTreatmentId(Long treatmentId);

    List<AssessmentDto> getAllAssessmentsByTreatment(Long treatmentId, boolean submissions) throws AssessmentNotMatchingException;

    AssessmentDto postAssessment(AssessmentDto assessmentDto, long treatmentId) throws IdInPostException, AssessmentNotMatchingException, DataServiceException, TitleValidationException;

    AssessmentDto toDto(Assessment assessment, boolean questions, boolean answers, boolean submissions, boolean student) throws AssessmentNotMatchingException;

    AssessmentDto toDto(Assessment assessment, Long submissionId, boolean questions, boolean answers,
            boolean submissions, boolean student) throws AssessmentNotMatchingException;

    Assessment fromDto(AssessmentDto assessmentDto) throws DataServiceException;

    Assessment save(Assessment assessment);

    Optional<Assessment> findById(Long id);

    Assessment getAssessment(Long id);

    void updateAssessment(Long id, AssessmentDto assessmentDto) throws TitleValidationException;

    void saveAndFlush(Assessment assessmentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean assessmentBelongsToExperimentAndConditionAndTreatment(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

    Float calculateMaxScore(Assessment assessment);

    void validateTitle(String title) throws TitleValidationException;

    AssessmentDto defaultAssessment(AssessmentDto assessmentDto, Long treatmentId);

    void updateTreatment(Long treatmentId, Assessment assessment);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);
}
