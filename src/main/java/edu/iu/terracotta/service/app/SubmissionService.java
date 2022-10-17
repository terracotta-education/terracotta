package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.NoSubmissionsException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.exceptions.SubmissionNotMatchingException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SubmissionService {

    List<Submission> findAllByAssessmentId(Long assessmentId);

    List<SubmissionDto> getSubmissions(Long experimentId, String userId, Long assessmentId, boolean student) throws NoSubmissionsException;

    Submission getSubmission(Long experimentId, String userId, Long submissionId, boolean student) throws NoSubmissionsException;

    SubmissionDto postSubmission(SubmissionDto submissionDto, long experimentId, SecuredInfo securedInfo, long assessmentId, boolean student) throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException;

    void updateSubmissions(Map<Submission, SubmissionDto> map, boolean student) throws ConnectionException, DataServiceException;

    SubmissionDto toDto(Submission submission, boolean questionSubmissions, boolean submissionComments);

    Submission fromDto(SubmissionDto submissionDto, boolean student) throws DataServiceException;

    Submission save(Submission submission);

    Optional<Submission> findById(Long id);

    List<Submission> findByParticipantId(Long participantId);

    Optional<Submission> findByParticipantIdAndSubmissionId(Long participantId, Long submissionId);

    List<Submission> findByParticipantIdAndAssessmentId(Long participantId, Long assessmentId);

    Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userId);

    void saveAndFlush(Submission submissionToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean submissionBelongsToAssessment(Long assessmentId, Long SubmissionId);

    void finalizeAndGrade(Long submissionId, SecuredInfo securedInfo, boolean student)
            throws DataServiceException, CanvasApiException, IOException, AssignmentDatesException, ConnectionException;

    void grade(Long submissionId, SecuredInfo securedInfo) throws DataServiceException;

    Submission gradeSubmission(Submission submission) throws DataServiceException;

    void sendSubmissionGradeToCanvasWithLTI(Submission submission, boolean studentSubmission)
            throws ConnectionException, DataServiceException, CanvasApiException, IOException;

    boolean datesAllowed(Long experimentId, Long treatmentId, SecuredInfo securedInfo);

    Submission createNewSubmission(Assessment assessment, Participant participant, SecuredInfo securedInfo);

    void validateUser(Long experimentId, String userId, Long submissionId) throws InvalidUserException;

    void validateDto(Long experimentId, String userId, SubmissionDto submissionDto) throws InvalidUserException, ParticipantNotMatchingException;

    HttpHeaders buildHeaders(UriComponentsBuilder ucBuilder, long experimentId, long conditionId, long treatmentId, long assessmentId, long submissionId);

    void allowedSubmission(Long submissionId, SecuredInfo securedInfo) throws SubmissionNotMatchingException;

    Float getScoreFromMultipleSubmissions(Participant participant, Assessment assessment);

    Float getSubmissionScore(Submission submission);

}
