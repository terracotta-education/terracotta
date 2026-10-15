package edu.iu.terracotta.service.app.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.model.dto.AssignmentFileArchiveDto;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.exceptions.AssignmentFileArchiveNotFoundException;
import edu.iu.terracotta.service.app.AssignmentFileArchiveService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AssignmentFileArchiveServiceImpl implements AssignmentFileArchiveService {

    private static final String ARCHIVE_NOT_FOUND_FOR_ASSIGNMENT = "No assignment file archive with assignment ID: [%s] found.";

    // an archive in one of these states is compared with the latest uploads; one still being built
    // or that failed isn't
    private static final EnumSet<AssignmentFileArchiveStatus> CHECKED_FOR_NEW_UPLOADS = EnumSet.of(
        AssignmentFileArchiveStatus.READY,
        AssignmentFileArchiveStatus.DOWNLOADED,
        AssignmentFileArchiveStatus.OUTDATED,
        AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED
    );

    private final AssignmentFileArchiveRepository assignmentFileArchiveRepository;
    private final LtiUserRepository ltiUserRepository;
    private final AnswerFileSubmissionRepository answerFileSubmissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final FileStorageService fileStorageService;
    private final AssignmentAsyncService asyncService;

    @Override
    public AssignmentFileArchiveDto process(Assignment assignment, SecuredInfo securedInfo) throws IOException {
        return process(assignment, securedInfo, AssignmentFileArchiveStatus.PROCESSING);
    }

    private AssignmentFileArchiveDto process(Assignment assignment, SecuredInfo securedInfo, AssignmentFileArchiveStatus assignmentFileArchiveStatus) throws IOException {
        LtiUserEntity owner = ltiUserRepository.findFirstByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
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
            .orElseThrow(() -> new AssignmentFileArchiveNotFoundException(String.format(ARCHIVE_NOT_FOUND_FOR_ASSIGNMENT, assignment.getAssignmentId())));

        if (!markIfOutdated(assignmentFileArchive)) {
            return toDto(assignmentFileArchive, false);
        }

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
    public List<AssignmentFileArchiveDto> findOutdated(long experimentId) throws IOException {
        List<AssignmentFileArchiveDto> outdated = new ArrayList<>();

        for (Assignment assignment : assignmentRepository.findByExposure_Experiment_ExperimentId(experimentId)) {
            Optional<AssignmentFileArchive> latest = assignmentFileArchiveRepository.findTopByAssignment_AssignmentIdOrderByCreatedAtDesc(assignment.getAssignmentId());

            if (latest.isPresent() && markIfOutdated(latest.get())) {
                outdated.add(toDto(latest.get(), false));
            }
        }

        return outdated;
    }

    @Override
    public void outdatedAcknowledge(UUID uuid, Assignment assignment) throws AssignmentFileArchiveNotFoundException {
        AssignmentFileArchive assignmentFileArchive = assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(uuid, assignment.getAssignmentId())
            .orElseThrow(() -> new AssignmentFileArchiveNotFoundException(String.format(ARCHIVE_NOT_FOUND_FOR_ASSIGNMENT, assignment.getAssignmentId())));

        if (AssignmentFileArchiveStatus.OUTDATED != assignmentFileArchive.getStatus()) {
            // nothing to dismiss
            return;
        }

        assignmentFileArchive.setStatus(AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED);
        assignmentFileArchiveRepository.save(assignmentFileArchive);
    }

    @Override
    public void errorAcknowledge(UUID uuid, Assignment assignment) throws IOException, AssignmentFileArchiveNotFoundException {
        AssignmentFileArchive assignmentFileArchive = assignmentFileArchiveRepository.findByUuidAndAssignment_AssignmentId(uuid, assignment.getAssignmentId())
            .orElseThrow(() -> new AssignmentFileArchiveNotFoundException(String.format(ARCHIVE_NOT_FOUND_FOR_ASSIGNMENT, assignment.getAssignmentId())));

        assignmentFileArchive.setStatus(AssignmentFileArchiveStatus.ERROR_ACKNOWLEDGED);
        assignmentFileArchiveRepository.save(assignmentFileArchive);
    }

    @Override
    public AssignmentFileArchiveDto toDto(AssignmentFileArchive assignmentFileArchive, boolean includeFileContent) throws IOException {
        return AssignmentFileArchiveDto.builder()
            .assignmentId(assignmentFileArchive.getAssignment().getUuid())
            .assignmentTitle(assignmentFileArchive.getAssignment().getTitle())
            .exposureId(assignmentFileArchive.getAssignment().getExposure().getUuid())
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

    /**
     * Whether the archive has uploads newer than it, marking it OUTDATED if so. One still being
     * built, or that failed, never is: marking it would be overwritten when the build finishes.
     */
    private boolean markIfOutdated(AssignmentFileArchive assignmentFileArchive) {
        if (!CHECKED_FOR_NEW_UPLOADS.contains(assignmentFileArchive.getStatus())) {
            return false;
        }

        // the instructor dismissed the "new uploads" indicator: keep it dismissed until there are
        // uploads newer than that (the acknowledgement is the archive's last update)
        Timestamp since = AssignmentFileArchiveStatus.OUTDATED_ACKNOWLEDGED == assignmentFileArchive.getStatus()
            ? assignmentFileArchive.getUpdatedAt()
            : assignmentFileArchive.getCreatedAt();

        if (!hasUploadsSince(assignmentFileArchive.getAssignmentId(), since)) {
            return false;
        }

        if (AssignmentFileArchiveStatus.OUTDATED != assignmentFileArchive.getStatus()) {
            assignmentFileArchive.setStatus(AssignmentFileArchiveStatus.OUTDATED);
            assignmentFileArchiveRepository.save(assignmentFileArchive);
        }

        return true;
    }

    private boolean isArchiveCurrent(AssignmentFileArchive assignmentFileArchive) {
        return !hasUploadsSince(assignmentFileArchive.getAssignmentId(), assignmentFileArchive.getCreatedAt());
    }

    // whether a consenting participant uploaded or replaced a file for the assignment after the given time
    private boolean hasUploadsSince(long assignmentId, Timestamp since) {
        Optional<Timestamp> latestUpload = answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(assignmentId);

        return latestUpload.isPresent() && latestUpload.get().after(since);
    }

}
