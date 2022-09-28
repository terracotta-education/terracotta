package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

public interface AssessmentService {

    List<Assessment> findAllByTreatmentId(Long treatmentId);

    List<AssessmentDto> getAllAssessmentsByTreatment(Long treatmentId, boolean submissions) throws AssessmentNotMatchingException;

    AssessmentDto postAssessment(AssessmentDto assessmentDto, long treatmentId)
                    throws IdInPostException, AssessmentNotMatchingException, DataServiceException,
                    TitleValidationException;

    AssessmentDto duplicateAssessment(long assessmentId, long treatmentId) throws DataServiceException, AssessmentNotMatchingException, TreatmentNotMatchingException;

    AssessmentDto duplicateAssessment(long assessmentId, Treatment treatment, Assignment assignment) throws DataServiceException, AssessmentNotMatchingException;

    AssessmentDto toDto(Assessment assessment, boolean questions, boolean answers, boolean submissions, boolean student) throws AssessmentNotMatchingException;

    AssessmentDto toDto(Assessment assessment, Long submissionId, boolean questions, boolean answers,
            boolean submissions, boolean student) throws AssessmentNotMatchingException;

    Assessment fromDto(AssessmentDto assessmentDto) throws DataServiceException;

    Assessment save(Assessment assessment);

    Optional<Assessment> findById(Long id);

    Assessment getAssessment(Long id);

    void updateAssessment(Long id, AssessmentDto assessmentDto)
                    throws TitleValidationException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException;

    void saveAndFlush(Assessment assessmentToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean assessmentBelongsToExperimentAndConditionAndTreatment(Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

    Float calculateMaxScore(Assessment assessment);

    void validateTitle(String title) throws TitleValidationException;

    AssessmentDto defaultAssessment(AssessmentDto assessmentDto, Long treatmentId);

    void updateTreatment(Long treatmentId, Assessment assessment);

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);

    Assessment getAssessmentForParticipant(Participant participant, SecuredInfo securedInfo) throws AssessmentNotMatchingException;

    Assessment getAssessmentByGroupId(Long experimentId, String canvasAssignmentId, Long groupId) throws AssessmentNotMatchingException;

    Assessment getAssessmentByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws AssessmentNotMatchingException;

    AssessmentDto viewAssessment(long expermientId, SecuredInfo securedInfo) throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException;

}
