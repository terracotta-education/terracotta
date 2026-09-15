package edu.iu.terracotta.service.app.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import edu.iu.terracotta.utils.TextConstants;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AssignmentTreatmentServiceImpl implements AssignmentTreatmentService {

    private final AssessmentRepository assessmentRepository;
    private final LtiUserRepository ltiUserRepository;
    private final SubmissionRepository submissionRepository;
    private final TreatmentRepository treatmentRepository;
    private final AssessmentService assessmentService;
    private final ApiClient apiClient;

    @PersistenceContext private EntityManager entityManager;

    // TreatmentController.duplicateTreatment (unlike AssignmentController.duplicateAssignment,
    // which wraps its whole request in @Transactional) has no transactional boundary of its own
    // and relies solely on open-in-view to hold one Hibernate Session across this entire
    // multi-step Treatment -> Assessment -> Question write chain. Declaring the transaction here,
    // at the service layer, protects every caller (both controllers, and the loop in
    // AssignmentServiceImpl.duplicateAssignment) consistently and atomically, regardless of
    // which one started/joins it - a REQUIRED transaction here just joins the caller's existing
    // one when there is one (e.g. AssignmentController's), so this is a no-op for that path and
    // only adds a boundary where one was otherwise missing.
    @Override
    @Transactional
    public TreatmentDto duplicateTreatment(long treatmentId, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            ApiException, TreatmentNotMatchingException, QuestionNotMatchingException, TerracottaConnectorException {
        return duplicateTreatment(treatmentId, null, securedInfo);
    }

    @Override
    @Transactional
    public TreatmentDto duplicateTreatment(long treatmentId, Assignment assignment, SecuredInfo securedInfo)
        throws IdInPostException, DataServiceException, ExceedingLimitException, AssessmentNotMatchingException, NumberFormatException,
            ApiException, TreatmentNotMatchingException, QuestionNotMatchingException, TerracottaConnectorException {
        Treatment from = treatmentRepository.findByTreatmentId(treatmentId);

        if (from == null) {
            throw new DataServiceException("The treatment with the given ID does not exist");
        }

        // findByTreatmentId queries by the @Id property, which Spring Data JPA/Hibernate can
        // resolve as an uninitialized reference/proxy rather than a fully-loaded entity when
        // this Treatment has not otherwise been touched in the current persistence context
        // (e.g. a fresh transaction against already-committed data - the normal case in
        // production, as opposed to a test that creates and duplicates data in one transaction).
        // Detaching and mutating (including resetting the identifier) a still-proxied entity
        // corrupts its identifier bookkeeping: the proxy keeps reporting its ORIGINAL id
        // internally even after being re-persisted under a new one, which later throws
        // Hibernate's "AssertionFailure: null identifier" when this object is used as a FK on a
        // dependent entity's insert (e.g. the new Assessment/Question rows below) - see the
        // identical fix/rationale in AssessmentServiceImpl.duplicateAssessment. Unproxying first
        // guarantees we detach and mutate the real, fully-initialized entity.
        from = (Treatment) Hibernate.unproxy(from);
        entityManager.detach(from);

        // reset ID and version - a non-zero, copied-over version makes Spring Data JPA's
        // isNew() check (which consults @Version before falling back to the ID) treat this
        // as an existing detached entity, routing save() through merge() instead of persist()
        // and leaving the IDENTITY-generated treatmentId unset when the new Assessment below
        // is persisted with a non-nullable FK to this Treatment (Hibernate then throws
        // AssertionFailure: null identifier while checking that association)
        from.setTreatmentId(null);
        from.setVersion(0);
        // the detached copy still carries the ORIGINAL row's uuid; clearing it lets
        // UuidAwareEntity's @PrePersist generate a fresh one for this new row instead of
        // colliding with the source row's unique uuid constraint
        from.setUuid(null);

        // set new assignment; if exists
        if (assignment != null) {
            from.setAssignment(assignment);
        }

        // unset the assessment
        from.setAssessment(null);

        // saveAndFlush (not save): duplicateAssessment below immediately uses newTreatment as
        // the FK for the new Assessment's own insert - see the identical fix/rationale for
        // Assessment in AssessmentServiceImpl.duplicateAssessment.
        Treatment newTreatment = treatmentRepository.saveAndFlush(from);
        LtiUserEntity instructorUser = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        setAssignmentDtoAttrs(newTreatment.getAssignment(), securedInfo.getLmsCourseId(), instructorUser);
        TreatmentDto treatmentDto = toTreatmentDto(newTreatment, false, true, securedInfo);

        // duplicate assessment
        List<Assessment> existingAssessments = assessmentRepository.findByTreatment_TreatmentId(treatmentId);

        if (CollectionUtils.isNotEmpty(existingAssessments)) {
            Assessment newAssessment = assessmentService.duplicateAssessment(existingAssessments.get(0).getAssessmentId(), newTreatment, assignment);
            newTreatment.setAssessment(newAssessment);
            treatmentRepository.saveAndFlush(newTreatment);
            treatmentDto.setAssessmentDto(assessmentService.toDto(newAssessment, true, true, true, false, securedInfo));
        }

        return treatmentDto;
    }

    @Override
    public TreatmentDto toTreatmentDto(Treatment treatment, boolean submissions, boolean addAssignmentDto, SecuredInfo securedInfo) throws AssessmentNotMatchingException {
        TreatmentDto treatmentDto = new TreatmentDto();

        treatmentDto.setTreatmentId(treatment.getTreatmentId());

        if (addAssignmentDto) {
            treatmentDto.setAssignmentDto(toAssignmentDto(treatment.getAssignment(), false, false, securedInfo));
        }

        if (treatment.getAssessment() != null) {
            treatmentDto.setAssessmentDto(assessmentService.toDto(treatment.getAssessment(), true, false, submissions, false, securedInfo));
        }

        treatmentDto.setConditionId(treatment.getCondition().getUuid());
        // keeping assignmentId at the root, as removal will break the UI in many places...
        treatmentDto.setAssignmentId(treatment.getAssignment().getAssignmentId());

        return treatmentDto;
    }

    @Override
    public AssignmentDto toAssignmentDto(Assignment assignment, boolean submissions, boolean addTreatmentDto, SecuredInfo securedInfo) throws AssessmentNotMatchingException {
        AssignmentDto assignmentDto = AssignmentDto.builder().build();
        assignmentDto.setAssignmentId(assignment.getAssignmentId());
        assignmentDto.setLmsAssignmentId(assignment.getLmsAssignmentId());
        assignmentDto.setTitle(assignment.getTitle());
        assignmentDto.setAssignmentOrder(assignment.getAssignmentOrder());
        assignmentDto.setExposureId(assignment.getExposure().getUuid());
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
            List<Treatment> treatments = treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(assignment.getAssignmentId());
            List<TreatmentDto> treatmentDtoList = new ArrayList<>();

            for (Treatment treatment : treatments) {
                TreatmentDto treatmentDto = toTreatmentDto(treatment, submissions, false, securedInfo);
                treatmentDtoList.add(treatmentDto);
            }

            assignmentDto.setTreatments(treatmentDtoList);
        }

        assignmentDto.setPublished(assignment.isPublished());
        assignmentDto.setDueDate(assignment.getDueDate());

        return assignmentDto;
    }

    @Override
    public List<AssignmentDto> toAssignmentDto(List<Assignment> assignments, boolean submissions, boolean addTreatmentDto, SecuredInfo securedInfo) throws AssessmentNotMatchingException {
        return assignments.stream()
            .map(assignment -> {
                try {
                    return toAssignmentDto(assignment, submissions, addTreatmentDto, securedInfo);
                } catch (AssessmentNotMatchingException e) {
                    log.error(TextConstants.ASSIGNMENT_NOT_MATCHING, e);

                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public void setAssignmentDtoAttrs(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws NumberFormatException, ApiException, TerracottaConnectorException {
        Optional<LmsAssignment> lmsAssignment = apiClient.listAssignment(instructorUser, lmsCourseId, assignment);

        if (lmsAssignment.isEmpty()) {
            return;
        }

        assignment.setPublished(lmsAssignment.get().isPublished());
        assignment.setDueDate(lmsAssignment.get().getDueAt());
    }

    @Override
    public void setAssignmentDtoAttrs(List<Assignment> assignments, LtiUserEntity instructorUser) throws ApiException, TerracottaConnectorException {
        if (CollectionUtils.isEmpty(assignments)) {
            return;
        }

        Map<String, LmsAssignment> lmsAssignmentsByLmsAssignmentId = apiClient.listAssignments(instructorUser, assignments.get(0).getExposure().getExperiment()).stream()
            .collect(Collectors.toMap(LmsAssignment::getId, lmsAssignment -> lmsAssignment, (first, second) -> first));

        for (Assignment assignment : assignments) {
            LmsAssignment lmsAssignment = lmsAssignmentsByLmsAssignmentId.get(assignment.getLmsAssignmentId());

            if (lmsAssignment == null) {
                continue;
            }

            assignment.setPublished(lmsAssignment.isPublished());
            assignment.setDueDate(lmsAssignment.getDueAt());
        }
    }

}
