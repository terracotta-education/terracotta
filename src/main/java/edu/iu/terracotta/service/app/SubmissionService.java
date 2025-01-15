package edu.iu.terracotta.service.app;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.dao.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.dto.SubmissionDto;
import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface SubmissionService {

    List<SubmissionDto> getSubmissions(Long experimentId, String userId, Long assessmentId, boolean student) throws NoSubmissionsException;
    Submission getSubmission(Long experimentId, String userId, Long submissionId, boolean student) throws NoSubmissionsException;
    SubmissionDto postSubmission(SubmissionDto submissionDto, long experimentId, SecuredInfo securedInfo, long assessmentId, boolean student) throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException, IntegrationTokenNotFoundException;
    void updateSubmissions(Map<Submission, SubmissionDto> map, boolean student) throws ConnectionException, DataServiceException, ApiException, IOException, TerracottaConnectorException;
    SubmissionDto toDto(Submission submission, boolean questionSubmissions, boolean submissionComments);
    Submission fromDto(SubmissionDto submissionDto, boolean student) throws DataServiceException;
    void deleteById(Long id) throws EmptyResultDataAccessException;
    void finalizeAndGrade(Long submissionId, SecuredInfo securedInfo, boolean student) throws DataServiceException, IOException, AssignmentDatesException, ConnectionException, ApiException, TerracottaConnectorException;
    void grade(Long submissionId, SecuredInfo securedInfo) throws DataServiceException;
    void sendSubmissionGradeToLmsWithLti(Submission submission, boolean studentSubmission) throws ConnectionException, DataServiceException, IOException, ApiException, TerracottaConnectorException;
    boolean datesAllowed(Long experimentId, Long treatmentId, SecuredInfo securedInfo);
    Submission createNewSubmission(Assessment assessment, Participant participant, SecuredInfo securedInfo) throws IntegrationTokenNotFoundException;
    void validateUser(Long experimentId, String userId, Long submissionId) throws InvalidUserException;
    void validateDto(Long experimentId, String userId, SubmissionDto submissionDto) throws InvalidUserException, ParticipantNotMatchingException;
    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId, long assessmentId, long submissionId);
    void allowedSubmission(Long submissionId, SecuredInfo securedInfo) throws SubmissionNotMatchingException;
    Float getScoreFromMultipleSubmissions(Participant participant, Assessment assessment);
    Float getSubmissionScore(Submission submission);
    boolean isManualGradingNeeded(Submission submission);

}
