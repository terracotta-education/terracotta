package edu.iu.terracotta.service.app.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.InvalidUserException;
import edu.iu.terracotta.exceptions.ParticipantNotMatchingException;
import edu.iu.terracotta.model.app.AnswerMc;
import edu.iu.terracotta.model.app.AnswerMcSubmissionOption;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Condition;
import edu.iu.terracotta.model.app.Experiment;
import edu.iu.terracotta.model.app.Participant;
import edu.iu.terracotta.model.app.QuestionMc;
import edu.iu.terracotta.model.app.QuestionSubmission;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.SubmissionDto;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.repository.AnswerMcRepository;
import edu.iu.terracotta.repository.AnswerMcSubmissionOptionRepository;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.ParticipantRepository;
import edu.iu.terracotta.repository.QuestionSubmissionRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.service.app.APIJWTService;
import edu.iu.terracotta.service.app.AssignmentService;

public class SubmissionServiceImplTest {

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Mock
    private AllRepositories allRepositories;

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private QuestionSubmissionRepository questionSubmissionRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AnswerMcRepository answerMcRepository;

    @Mock
    private AnswerMcSubmissionOptionRepository answerMcSubmissionOptionRepository;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private APIJWTService apijwtService;

    @Mock
    private Assignment assignment;

    @Mock
    private Assessment assessment;

    @Mock
    private Treatment treatment;

    @Mock
    private Participant participant;

    @Mock
    private Condition condition;

    @Mock
    private Experiment experiment;

    @Mock
    private QuestionMc question;

    @Mock
    private QuestionSubmission questionSubmission;

    @Mock
    private AnswerMc answerMc;

    @Mock
    private SecuredInfo securedInfo;

    private Submission submission;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        clearInvocations(assignmentRepository);
        allRepositories.answerMcRepository = answerMcRepository;
        allRepositories.answerMcSubmissionOptionRepository = answerMcSubmissionOptionRepository;
        allRepositories.assessmentRepository = assessmentRepository;
        allRepositories.assignmentRepository = assignmentRepository;
        allRepositories.participantRepository = participantRepository;
        allRepositories.questionSubmissionRepository = questionSubmissionRepository;
        allRepositories.submissionRepository = submissionRepository;

        submission = new Submission();
        submission.setParticipant(participant);
        submission.setAssessment(assessment);

        when(allRepositories.submissionRepository.save(any(Submission.class))).thenReturn(submission);
        when(allRepositories.participantRepository.findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(anyLong(), anyString())).thenReturn(participant);
        when(allRepositories.participantRepository.findById(anyLong())).thenReturn(Optional.of(participant));
        when(allRepositories.assessmentRepository.findById(anyLong())).thenReturn(Optional.of(assessment));
        when(assignmentService.save(any(Assignment.class))).thenReturn(assignment);
        when(allRepositories.questionSubmissionRepository.save(any(QuestionSubmission.class))).thenReturn(questionSubmission);
        when(allRepositories.answerMcRepository.findByQuestion_QuestionId(anyLong())).thenReturn(Collections.singletonList(answerMc));
        when(allRepositories.answerMcSubmissionOptionRepository.save(any(AnswerMcSubmissionOption.class))).thenReturn(null);
        when(assessment.getQuestions()).thenReturn(Collections.singletonList(question));
        when(assessment.getTreatment()).thenReturn(treatment);
        when(treatment.getAssignment()).thenReturn(assignment);
        when(treatment.getCondition()).thenReturn(condition);
        when(condition.getExperiment()).thenReturn(experiment);
        when(question.getQuestionType()).thenReturn(QuestionTypes.MC);
        when((question).isRandomizeAnswers()).thenReturn(true);
        when(apijwtService.isTestStudent(any(SecuredInfo.class))).thenReturn(false);
        when(securedInfo.getUserId()).thenReturn("canvasUserId");
    }

    @Test
    public void testPostSubmissionNotStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException {
        submissionService.postSubmission(new SubmissionDto(), 0l, securedInfo, 0l, false);

        verify(assignmentService).save(assignment);
    }

    @Test
    public void testPostSubmissionAlreadyStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException {
        when(assignment.isStarted()).thenReturn(true);
        submissionService.postSubmission(new SubmissionDto(), 0l, securedInfo, 0l, false);

        verify(assignmentService, never()).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionNotStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException {
        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentService).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionAlreadyStarted() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException {
        when(assignment.isStarted()).thenReturn(true);
        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentService, never()).save(assignment);
    }

    @Test
    public void testCreateNewSubmissionTestStudent() throws IdInPostException, ParticipantNotMatchingException, InvalidUserException, DataServiceException {
        when(apijwtService.isTestStudent(any(SecuredInfo.class))).thenReturn(true);
        submissionService.createNewSubmission(assessment, participant, securedInfo);

        verify(assignmentService, never()).save(assignment);
    }

}
