package edu.iu.terracotta.service.app.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.AssessmentNotMatchingException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.ExceedingLimitException;
import edu.iu.terracotta.exceptions.IdInPostException;
import edu.iu.terracotta.exceptions.QuestionNotMatchingException;
import edu.iu.terracotta.exceptions.TreatmentNotMatchingException;
import edu.iu.terracotta.model.LtiUserEntity;
import edu.iu.terracotta.model.app.Assessment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.Treatment;
import edu.iu.terracotta.model.app.dto.AssignmentDto;
import edu.iu.terracotta.model.app.dto.TreatmentDto;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.AssessmentRepository;
import edu.iu.terracotta.repository.LtiUserRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import edu.iu.terracotta.repository.TreatmentRepository;
import edu.iu.terracotta.service.app.AssessmentService;
import edu.iu.terracotta.service.app.AssignmentTreatmentService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;

@Service
public class AssignmentTreatmentServiceImpl implements AssignmentTreatmentService {

    @Autowired private AssessmentRepository assessmentRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AssessmentService assessmentService;
    @Autowired private CanvasAPIClient canvasAPIClient;

    @PersistenceContext private EntityManager entityManager;

    @Override
    public TreatmentDto duplicateTreatment(long treatmentId, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
        return duplicateTreatment(treatmentId, null, securedInfo);
    }

    @Override
    public TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            CanvasApiException, TreatmentNotMatchingException, QuestionNotMatchingException {
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
        setAssignmentDtoAttrs(newTreatment.getAssignment(), securedInfo.getCanvasCourseId(), instructorUser);
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

        long submissionsCount = submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());

        if (submissionsCount > 0) {
            assignmentDto.setStarted(true);
        }

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
    public void setAssignmentDtoAttrs(Assignment assignment, String canvasCourseId, LtiUserEntity instructorUser) throws NumberFormatException, CanvasApiException {
        Optional<AssignmentExtended> canvasAssignment = canvasAPIClient.listAssignment(instructorUser, canvasCourseId, Integer.parseInt(assignment.getLmsAssignmentId()));

        if (canvasAssignment.isEmpty()) {
            return;
        }

        assignment.setPublished(canvasAssignment.get().isPublished());
        assignment.setDueDate(canvasAssignment.get().getDueAt());
    }

}
