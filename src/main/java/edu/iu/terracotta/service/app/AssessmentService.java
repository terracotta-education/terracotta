package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.RegradeDetails;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssessmentDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

public interface AssessmentService {

    List<AssessmentDto> getAllAssessmentsByTreatment(Long treatmentId, boolean submissions) throws AssessmentNotMatchingException;
    AssessmentDto postAssessment(AssessmentDto assessmentDto, long treatmentId) throws IdInPostException, AssessmentNotMatchingException, DataServiceException, TitleValidationException;
    Assessment duplicateAssessment(long assessmentId, long treatmentId) throws DataServiceException, AssessmentNotMatchingException, TreatmentNotMatchingException, QuestionNotMatchingException;
    Assessment duplicateAssessment(long assessmentId, Treatment treatment, Assignment assignment) throws DataServiceException, AssessmentNotMatchingException, QuestionNotMatchingException;
    AssessmentDto toDto(Assessment assessment, boolean questions, boolean answers, boolean submissions, boolean isStudent) throws AssessmentNotMatchingException;
    AssessmentDto toDto(Assessment assessment, Long submissionId, boolean questions, boolean answers, boolean submissions, boolean isStudent) throws AssessmentNotMatchingException;
    Assessment fromDto(AssessmentDto assessmentDto) throws DataServiceException;
    Assessment getAssessment(Long id);
    AssessmentDto putAssessment(Long id, AssessmentDto assessmentDto, boolean processQuestions)
                    throws TitleValidationException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException;
    Assessment updateAssessment(Long id, AssessmentDto assessmentDto, boolean processQuestions)
                    throws TitleValidationException, RevealResponsesSettingValidationException,
                    MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, IdInPostException, DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    AssessmentDto defaultAssessment(AssessmentDto assessmentDto, Long treatmentId);
    void updateTreatment(Long treatmentId, Assessment assessment);
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);
    Assessment getAssessmentForParticipant(Participant participant, SecuredInfo securedInfo) throws AssessmentNotMatchingException;
    Assessment getAssessmentByGroupId(Long experimentId, String canvasAssignmentId, Long groupId) throws AssessmentNotMatchingException;
    Assessment getAssessmentByConditionId(Long experimentId, String canvasAssignmentId, Long conditionId) throws AssessmentNotMatchingException;
    AssessmentDto viewAssessment(long expermientId, SecuredInfo securedInfo) throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException, AssignmentNotMatchingException, DataServiceException, CanvasApiException, IOException, AssignmentDatesException, ConnectionException;
    void verifySubmissionLimit(Integer limit, int existingSubmissionsCount) throws AssignmentAttemptException;
    void verifySubmissionWaitTime(Float waitTime, List<Submission> submissionList) throws AssignmentAttemptException;
    void regradeQuestions(RegradeDetails regradeDetails, long assessmentId) throws DataServiceException, ConnectionException, CanvasApiException, IOException;

}
