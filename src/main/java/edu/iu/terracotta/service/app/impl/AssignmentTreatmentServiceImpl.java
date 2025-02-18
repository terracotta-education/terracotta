package edu.iu.terracotta.service.app.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.Assessment;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.dao.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.dao.model.dto.AssignmentDto;
import edu.iu.terracotta.dao.model.dto.TreatmentDto;
import edu.iu.terracotta.dao.repository.AssessmentRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;

@Service
public class AssignmentTreatmentServiceImpl implements AssignmentTreatmentService {

    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AssessmentService assessmentService;
    @Autowired private ApiClient apiClient;

    @PersistenceContext private EntityManager entityManager;

    @Override
    public TreatmentDto duplicateTreatment(long treatmentId, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            ApiException, TreatmentNotMatchingException, QuestionNotMatchingException, TerracottaConnectorException {
        return duplicateTreatment(treatmentId, null, securedInfo);
    }

    @Override
    public TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            ApiException, TreatmentNotMatchingException, QuestionNotMatchingException, TerracottaConnectorException {
        Treatment from = treatmentRepository.findByTreatmentId(treatmentId);

        if (from == null) {
            throw new DataServiceException("The treatment with the given ID does not exist");
        }

        entityManager.detach(from);

        // reset ID
        from.setTreatmentId(null);

        // set new assignment; if exists
        if (assignment != null) {
            from.setAssignment(assignment);
        }

        // unset the assessment
        from.setAssessment(null);

        Treatment newTreatment = treatmentRepository.save(from);
        LtiUserEntity instructorUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        setAssignmentDtoAttrs(newTreatment.getAssignment(), securedInfo.getLmsCourseId(), instructorUser);
        TreatmentDto treatmentDto = toTreatmentDto(newTreatment, false, true);

        // duplicate assessment
        List<Assessment> existingAssessments = assessmentRepository.findByTreatment_TreatmentId(treatmentId);

        if (CollectionUtils.isNotEmpty(existingAssessments)) {
            Assessment newAssessment = assessmentService.duplicateAssessment(existingAssessments.get(0).getAssessmentId(), newTreatment, assignment);
            newTreatment.setAssessment(newAssessment);
            treatmentRepository.saveAndFlush(newTreatment);
            treatmentDto.setAssessmentDto(assessmentService.toDto(newAssessment, true, true, true, false));
        }

        return treatmentDto;
    }

    @Override
    public TreatmentDto toTreatmentDto(Treatment treatment, boolean submissions, boolean addAssignmentDto) throws AssessmentNotMatchingException {
        TreatmentDto treatmentDto = new TreatmentDto();

        treatmentDto.setTreatmentId(treatment.getTreatmentId());

        if (addAssignmentDto) {
            treatmentDto.setAssignmentDto(toAssignmentDto(treatment.getAssignment(), false, false));
        }

        if (treatment.getAssessment() != null) {
            treatmentDto.setAssessmentDto(assessmentService.toDto(treatment.getAssessment(), true, false, submissions, false));
        }

        treatmentDto.setConditionId(treatment.getCondition().getConditionId());
        // keeping assignmentId at the root, as removal will break the UI in many places...
        treatmentDto.setAssignmentId(treatment.getAssignment().getAssignmentId());

        return treatmentDto;
    }

    @Override
    public AssignmentDto toAssignmentDto(Assignment assignment, boolean submissions, boolean addTreatmentDto) throws AssessmentNotMatchingException {
        AssignmentDto assignmentDto = new AssignmentDto();
        assignmentDto.setAssignmentId(assignment.getAssignmentId());
        assignmentDto.setLmsAssignmentId(assignment.getLmsAssignmentId());
        assignmentDto.setTitle(assignment.getTitle());
        assignmentDto.setAssignmentOrder(assignment.getAssignmentOrder());
        assignmentDto.setExposureId(assignment.getExposure().getExposureId());
        assignmentDto.setResourceLinkId(assignment.getResourceLinkId());
        assignmentDto.setSoftDeleted(assignment.getSoftDeleted());
        assignmentDto.setNumOfSubmissions(assignment.getNumOfSubmissions());
        assignmentDto.setHoursBetweenSubmissions(assignment.getHoursBetweenSubmissions());
        assignmentDto.setMultipleSubmissionScoringScheme(assignment.getMultipleSubmissionScoringScheme().name());
        assignmentDto.setCumulativeScoringInitialPercentage(assignment.getCumulativeScoringInitialPercentage());
        assignmentDto.setAllowStudentViewResponses(assignment.isAllowStudentViewResponses());
        assignmentDto.setStudentViewResponsesAfter(assignment.getStudentViewResponsesAfter());
        assignmentDto.setStudentViewResponsesBefore(assignment.getStudentViewResponsesBefore());
        assignmentDto.setAllowStudentViewCorrectAnswers(assignment.isAllowStudentViewCorrectAnswers());
        assignmentDto.setStudentViewCorrectAnswersAfter(assignment.getStudentViewCorrectAnswersAfter());
        assignmentDto.setStudentViewCorrectAnswersBefore(assignment.getStudentViewCorrectAnswersBefore());

        List<Submission> assignmentSubmissions = CollectionUtils.emptyIfNull(submissionRepository.findByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId())).stream()
            .filter(submission -> !submission.getParticipant().getLtiUserEntity().isTestStudent())
            .toList();

        assignmentDto.setStarted(CollectionUtils.isNotEmpty(assignmentSubmissions));

        if (addTreatmentDto) {
            List<Treatment> treatments = treatmentRepository.findByAssignment_AssignmentId(assignment.getAssignmentId());
            List<TreatmentDto> treatmentDtoList = new ArrayList<>();

            for (Treatment treatment : treatments) {
                TreatmentDto treatmentDto = toTreatmentDto(treatment, submissions, false);
                treatmentDtoList.add(treatmentDto);
            }

            assignmentDto.setTreatments(treatmentDtoList);
        }

        assignmentDto.setPublished(assignment.isPublished());
        assignmentDto.setDueDate(assignment.getDueDate());

        return assignmentDto;
    }

    @Override
    public void setAssignmentDtoAttrs(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws NumberFormatException, ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> lmsAssignment = apiClient.listAssignment(instructorUser, lmsCourseId, assignment.getLmsAssignmentId());

        if (lmsAssignment.isEmpty()) {
            return;
        }

        assignment.setPublished(lmsAssignment.get().isPublished());
        assignment.setDueDate(lmsAssignment.get().getDueAt());
    }

}
