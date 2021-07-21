package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.AssignmentDatesException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface SubmissionService {

    List<Submission> findAllByAssessmentId(Long assessmentId);

    SubmissionDto toDto(Submission submission, boolean questionSubmissions, boolean submissionComments);

    Submission fromDto(SubmissionDto submissionDto) throws DataServiceException;

    Submission save(Submission submission);

    Optional<Submission> findById(Long id);

    List<Submission> findByParticipantId(Long participantId);

    Optional<Submission> findByParticipantIdAndSubmissionId(Long participantId, Long submissionId);

    List<Submission> findByParticipantIdAndAssessmentId(Long participantId, Long assessmentId);

    Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userId);

    void saveAndFlush(Submission submissionToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean submissionBelongsToAssessment(Long assessmentId, Long SubmissionId);

    void finalizeAndGrade(Long submissionId, SecuredInfo securedInfo) throws DataServiceException, CanvasApiException, IOException, AssignmentDatesException;

    void grade(Long submissionId, SecuredInfo securedInfo) throws DataServiceException;

    Submission gradeSubmission(Submission submission) throws DataServiceException;

    void sendSubmissionGradeToCanvas(Submission submission) throws ConnectionException, DataServiceException, CanvasApiException, IOException;

    boolean datesAllowed(Long experimentId, Long treatmentId, SecuredInfo securedInfo);

    Submission createNewSubmission(Assessment assessment, Participant participant, SecuredInfo securedInfo);
}
