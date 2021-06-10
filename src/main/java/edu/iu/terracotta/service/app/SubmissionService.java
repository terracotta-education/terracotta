package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.oauth2.SecurityInfo;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Optional;

public interface SubmissionService {

    List<Submission> findAllByAssessmentId(Long assessmentId);

    SubmissionDto toDto(Submission submission, boolean questionSubmissions, boolean submissionComments);

    Submission fromDto(SubmissionDto submissionDto) throws DataServiceException;

    Submission save(Submission submission);

    Optional<Submission> findById(Long id);

    Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userId);

    void saveAndFlush(Submission submissionToChange);

    void deleteById(Long id) throws EmptyResultDataAccessException;

    boolean submissionBelongsToAssessment(Long assessmentId, Long SubmissionId);

    void finalizeAndGrade(Long submissionId, SecurityInfo securityInfo) throws DataServiceException;

    void grade(Long submissionId, SecurityInfo securityInfo) throws DataServiceException;

    Submission gradeSubmission(Submission submission);
}
