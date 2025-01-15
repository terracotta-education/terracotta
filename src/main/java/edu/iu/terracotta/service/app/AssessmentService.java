package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.RegradeDetails;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.AssignmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.GroupNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationClientNotFoundException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationNotFoundException;
import edu.iu.terracotta.dao.model.dto.AssessmentDto;
import edu.iu.terracotta.exceptions.AssignmentAttemptException;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.MultipleAttemptsSettingsValidationException;
import edu.iu.terracotta.exceptions.MultipleChoiceLimitReachedException;
import edu.iu.terracotta.exceptions.NegativePointsException;
import edu.iu.terracotta.exceptions.RevealResponsesSettingValidationException;
import edu.iu.terracotta.exceptions.TitleValidationException;

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
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, IdInPostException,
        DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException;
    Assessment updateAssessment(Long id, AssessmentDto assessmentDto, boolean processQuestions)
        throws TitleValidationException, RevealResponsesSettingValidationException, MultipleAttemptsSettingsValidationException, AssessmentNotMatchingException, IdInPostException,
        DataServiceException, NegativePointsException, QuestionNotMatchingException, MultipleChoiceLimitReachedException, IntegrationClientNotFoundException, IntegrationNotFoundException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    AssessmentDto defaultAssessment(AssessmentDto assessmentDto, Long treatmentId);
    void updateTreatment(Long treatmentId, Assessment assessment);
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, Long experimentId, Long conditionId, Long treatmentId, Long assessmentId);
    Assessment getAssessmentForParticipant(Participant participant, SecuredInfo securedInfo) throws AssessmentNotMatchingException;
    Assessment getAssessmentByGroupId(Long experimentId, String lmsAssignmentId, Long groupId) throws AssessmentNotMatchingException;
    Assessment getAssessmentByConditionId(Long experimentId, String lmsAssignmentId, Long conditionId) throws AssessmentNotMatchingException;
    AssessmentDto viewAssessment(long expermientId, SecuredInfo securedInfo)
        throws ExperimentNotMatchingException, ParticipantNotMatchingException, AssessmentNotMatchingException, GroupNotMatchingException, ParticipantNotUpdatedException,
        AssignmentNotMatchingException, DataServiceException, IOException, AssignmentDatesException, ConnectionException, ApiException, TerracottaConnectorException;
    void verifySubmissionLimit(Integer limit, int existingSubmissionsCount) throws AssignmentAttemptException;
    void verifySubmissionWaitTime(Float waitTime, List<Submission> submissionList) throws AssignmentAttemptException;
    void regradeQuestions(RegradeDetails regradeDetails, long assessmentId) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException;

}
