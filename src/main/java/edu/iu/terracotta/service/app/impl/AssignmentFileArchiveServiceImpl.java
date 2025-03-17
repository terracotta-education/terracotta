package edu.iu.terracotta.service.app.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;
import edu.iu.terracotta.service.app.AssignmentFileArchiveService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AssignmentFileArchiveServiceImpl implements AssignmentFileArchiveService {

    @Autowired private AssignmentFileArchiveRepository assignmentFileArchiveRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private AssignmentAsyncService asyncService;

    @Override
    public AssignmentFileArchiveDto process(Assignment assignment, SecuredInfo securedInfo) throws IOException {
        return process(assignment, securedInfo, AssignmentFileArchiveStatus.PROCESSING);
    }

    private AssignmentFileArchiveDto process(Assignment assignment, SecuredInfo securedInfo, AssignmentFileArchiveStatus assignmentFileArchiveStatus) throws IOException {
        LtiUserEntity owner = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        log.info("User with ID: [{}] is processing assignment file archive for assignment with ID: [{}].", owner.getUserId(), assignment.getAssignmentId());
        AssignmentFileArchive assignmentFileArchive = AssignmentFileArchive.builder()
            .assignment(assignment)
            .owner(owner)
            .status(assignmentFileArchiveStatus)
            .build();

        assignmentFileArchive = assignmentFileArchiveRepository.save(assignmentFileArchive);

        asyncService.processAssignmentFileArchive(assignmentFileArchive);

        log.info("Assignment file archive with ID: [{}] is being processed.", assignmentFileArchive.getUuid());
        return toDto(assignmentFileArchive, false);
    }

    @Override
    public AssignmentFileArchiveDto poll(Assignment assignment, SecuredInfo securedInfo, boolean createNewOnOutdated) throws IOException, AssignmentFileArchiveNotFoundException {
        AssignmentFileArchive assignmentFileArchive = assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(assignment.getAssignmentId())
            .orElseThrow(() -> new AssignmentFileArchiveNotFoundException(String.format("No assignment file archive with assignment ID: [%s] found.", assignment.getAssignmentId())));

        // if archive is current, return it; else process a new one
        if (isArchiveCurrent(assignmentFileArchive)) {
            return toDto(assignmentFileArchive, false);
        }

        assignmentFileArchive.setStatus(AssignmentFileArchiveStatus.OUTDATED);
        assignmentFileArchiveRepository.save(assignmentFileArchive);

        if (!createNewOnOutdated) {
            return toDto(assignmentFileArchive, false);
        }

        return process(assignment, securedInfo, AssignmentFileArchiveStatus.REPROCESSING);
    }

    @Override
    public AssignmentFileArchiveDto retrieve(UUID uuid, Assignment assignment, SecuredInfo securedInfo) throws IOException {
        Optional<AssignmentFileArchive> assignmentFileArchive = findLatestAvailableArchive(assignment.getAssignmentId());

        if (assignmentFileArchive.isPresent()) {
            // existing valid assignment file archive found; return it
            assignmentFileArchive.get().setStatus(AssignmentFileArchiveStatus.DOWNLOADED);
            assignmentFileArchiveRepository.save(assignmentFileArchive.get());

            return toDto(assignmentFileArchive.get(), true);
        }

        // no existing valid assignment file archive found; process a new one
        return process(assignment, securedInfo);
    }

    @Override
    public Optional<AssignmentFileArchive> findLatestAvailableArchive(long assignmentId) throws IOException {
        Optional<AssignmentFileArchive> assignmentFileArchive = assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdAndStatusInOrderByCreatedAtDesc(
            assignmentId,
            Arrays.asList(
                AssignmentFileArchiveStatus.DOWNLOADED,
                AssignmentFileArchiveStatus.READY
            )
        );

        if (assignmentFileArchive.isEmpty()) {
            // No available assignment file archive found for the assignment
            return Optional.empty();
        }

        return isArchiveCurrent(assignmentFileArchive.get()) ? assignmentFileArchive : Optional.empty();
    }

    @Override
    public void errorAcknowledge(UUID uuid, Assignment assignment) throws IOException, AssignmentFileArchiveNotFoundException {
        AssignmentFileArchive assignmentFileArchive = assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(uuid, assignment.getAssignmentId())
            .orElseThrow(() -> new AssignmentFileArchiveNotFoundException(String.format("No assignment file archive with assignment ID: [%s] found.", assignment.getAssignmentId())));

        assignmentFileArchive.setStatus(AssignmentFileArchiveStatus.ERROR_ACKNOWLEDGED);
        assignmentFileArchiveRepository.save(assignmentFileArchive);
    }

    @Override
    public AssignmentFileArchiveDto toDto(AssignmentFileArchive assignmentFileArchive, boolean includeFileContent) throws IOException {
        return AssignmentFileArchiveDto.builder()
            .assignmentId(assignmentFileArchive.getAssignmentId())
            .assignmentTitle(assignmentFileArchive.getAssignment().getTitle())
            .experimentTitle(assignmentFileArchive.getExperimentTitle())
            .id(assignmentFileArchive.getUuid())
            .file(includeFileContent ? fileStorageService.getAssignmentFileArchive(assignmentFileArchive.getId()) : null)
            .fileName(
                StringUtils.isNotBlank(assignmentFileArchive.getFileName()) ?
                    String.format(
                        "%s%s",
                        assignmentFileArchive.getFileName(),
                        AssignmentFileArchive.COMPRESSED_FILE_EXTENSION
                    )
                    : null
            )
            .mimeType(assignmentFileArchive.getMimeType())
            .status(assignmentFileArchive.getStatus())
            .build();
    }

    private boolean isArchiveCurrent(AssignmentFileArchive assignmentFileArchive) {
        // get latest submission for the assignment
        Optional<Submission> submission = submissionRepository.findTopByAssessment_Treatment_Assignment_AssignmentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(assignmentFileArchive.getAssignmentId());

        // no submissions or archive is older than the latest submission
        return submission.isEmpty() || assignmentFileArchive.getCreatedAt().after(submission.get().getDateSubmitted());
    }

}
