package edu.iu.terracotta.service.app.async.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiContextRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.dao.entity.AnswerFileSubmission;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.Question;
import edu.iu.terracotta.dao.entity.Treatment;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.AnswerFileSubmissionRepository;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ObsoleteAssignmentRepository;
import edu.iu.terracotta.dao.repository.ParticipantRepository;
import edu.iu.terracotta.dao.repository.TreatmentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.async.AsyncService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AsyncServiceImpl implements AsyncService {

    @Autowired private AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Autowired private AssignmentFileArchiveRepository assignmentFileArchiveRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private ObsoleteAssignmentRepository obsoleteAssignmentRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private AssignmentService assignmentService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ApiClient apiClient;

    @PersistenceContext private EntityManager entityManager;

    @Value("${assignment.file.archive.local.path.root}")
    private String assignmentFileArchiveLocalPathRoot;

    @Async
    @Override
    @Transactional(rollbackFor = { ApiException.class })
    public void checkAndRestoreAssignmentsInLmsByContext(SecuredInfo securedInfo) throws ApiException, DataServiceException, ConnectionException, IOException, TerracottaConnectorException {
        List<Assignment> assignmentsToCheck = assignmentRepository.findAssignmentsToCheckByContext(securedInfo.getContextId());

        if (CollectionUtils.isEmpty(assignmentsToCheck)) {
            log.info("No assignments exist in Terracotta for context ID: [{}] to check in the LMS to recreate. Aborting.", securedInfo.getContextId());
            return;
        }

        log.info("Checking Terracotta assignment IDs for context ID: [{}] in the LMS: [{}]",
            securedInfo.getContextId(),
            Arrays.toString(assignmentsToCheck.stream().map(Assignment::getLmsAssignmentId).toArray())
        );

        List<LmsAssignment> lmsAssignments = assignmentService.getAllAssignmentsForLmsCourse(securedInfo);

        if (CollectionUtils.isEmpty(lmsAssignments)) {
            log.info("No assignments exist in LMS for context ID: [{}]. Aborting.", securedInfo.getContextId());
            return;
        }

        List<String> lmsAssignmentIds = lmsAssignments.stream()
            .map(LmsAssignment::getId)
            .toList();

        List<String> assignmentsRecreated = assignmentsToCheck.stream()
            .filter(assignmentToCheck -> !lmsAssignmentIds.contains(assignmentToCheck.getLmsAssignmentId()))
            .map(
                assignmentToCreate -> {
                    log.info("Creating assignment with ID: [{}] in the LMS ", assignmentToCreate.getAssignmentId());

                    try {
                        assignmentService.restoreAssignmentInLms(assignmentToCreate);
                    } catch (ApiException | DataServiceException | ConnectionException | IOException | TerracottaConnectorException e) {
                        log.error("Error restoring assignment with ID: [{}] in the LMS", assignmentToCreate.getAssignmentId(), e);
                    }

                    return Long.toString(assignmentToCreate.getAssignmentId());
                }
            )
            .toList();

        log.info("Checking Terracotta assignments for context ID: [{}] in LMS COMPLETE. Assignments recreated: [{}].",
            securedInfo.getContextId(),
            CollectionUtils.isNotEmpty(assignmentsRecreated) ?
                assignmentsRecreated.stream()
                    .collect(Collectors.joining(", ")) :
                "N/A"
        );
    }

    @Async
    @Override
    @Transactional(rollbackFor = { ApiException.class })
    public void handleObsoleteAssignmentsInLmsByContext(SecuredInfo securedInfo) throws DataServiceException, ConnectionException, IOException, ApiException, TerracottaConnectorException {
        // get assignments that currently exist in Terracotta for this context
        List<Assignment> terracottaAssignments = assignmentRepository.findAssignmentsToCheckByContext(securedInfo.getContextId());

        log.info("Checking for obsolete Terracotta assignments in LMS for context ID: [{}]", securedInfo.getContextId());

        List<String> terracottaLmsAssignmentIds = terracottaAssignments.stream()
            .map(Assignment::getLmsAssignmentId)
            .toList();

        // get lms assignments that are external tools that do not exist in Terracotta
        List<LmsAssignment> lmsAssignments = assignmentService.getAllAssignmentsForLmsCourse(securedInfo).stream()
            .filter(lmsAssignment -> !terracottaLmsAssignmentIds.contains(lmsAssignment.getId()))
            .filter(lmsAssignment -> lmsAssignment.getLmsExternalToolFields() != null)
            .toList();

        if (CollectionUtils.isEmpty(lmsAssignments)) {
            log.info("No assignments exist in LMS for context ID: [{}] to check for obsolescence. Aborting.", securedInfo.getContextId());
            return;
        }

        // get experiments that currently exist in Terracotta for this context
        List<Long> terracottaExperimentIds = experimentRepository.findAllByLtiContextEntity_ContextId(securedInfo.getContextId()).stream()
            .map(Experiment::getExperimentId)
            .toList();

        List<String> convertedLmsAssignmentIds = obsoleteAssignmentRepository.findAllByContext_ContextId(securedInfo.getContextId()).stream()
            .map(ObsoleteAssignment::getLmsAssignmentId)
            .toList();

        LtiUserEntity apiUser = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        LtiContextEntity ltiContext = ltiContextRepository.findById(securedInfo.getContextId())
            .orElseThrow(() -> new DataServiceException(String.format("LTI context ID: [%s] not found.", securedInfo.getContextId())));
        String localUrl = apiUser.getPlatformDeployment().getLocalUrl();

        // process assignments that do not exist in Terracotta, have not been converted already, and are external tools linked to this server
        List<String> obsoleteAssignmentIds = lmsAssignments.stream()
            .filter(lmsAssignment -> !terracottaLmsAssignmentIds.contains(lmsAssignment.getId()))
            .filter(lmsAssignment -> !convertedLmsAssignmentIds.contains(lmsAssignment.getId()))
            .filter(lmsAssignment -> StringUtils.containsIgnoreCase(lmsAssignment.getLmsExternalToolFields().getUrl(), localUrl))
            .map(lmsAssignment -> {
                try {
                    String[] queryParameters = StringUtils.split(URI.create(lmsAssignment.getLmsExternalToolFields().getUrl()).getQuery(), '&');

                    if (ArrayUtils.isEmpty(queryParameters)) {
                        // no query parameters; skip
                        return null;
                    }

                    // find the experiment ID from the query parameters
                    Optional<String> experimentId = Arrays.stream(queryParameters)
                        .filter(queryParameter -> StringUtils.equalsIgnoreCase(StringUtils.split(queryParameter, '=')[0], "experiment"))
                        .map(queryParameter -> StringUtils.split(queryParameter, '=')[1])
                        .findFirst();

                    if (experimentId.isEmpty()) {
                        // no experiment query parameter; skip
                        return null;
                    }

                    if (terracottaExperimentIds.contains(Long.parseLong(experimentId.get()))) {
                        // experiment ID is in this context; skip
                        return null;
                    }

                    ObsoleteAssignment obsoleteAssignment = ObsoleteAssignment.builder()
                        .context(ltiContext)
                        .lmsAssignmentId(lmsAssignment.getId())
                        .originalTitle(lmsAssignment.getName())
                        .originalUrl(lmsAssignment.getLmsExternalToolFields() != null ? lmsAssignment.getLmsExternalToolFields().getUrl() : "N/A")
                        .build();

                    // update name with "OBSOLETE" prefix
                    lmsAssignment.setName(String.format("%s %s", ObsoleteAssignment.PREFIX, lmsAssignment.getName()));

                    // update URL to obsolete assignment page
                    lmsAssignment.getLmsExternalToolFields().setUrl(String.format("%s/%s", localUrl, ObsoleteAssignment.URL));

                    apiClient.editAssignment(apiUser, lmsAssignment, securedInfo.getLmsCourseId());

                    obsoleteAssignmentRepository.save(obsoleteAssignment);

                    return lmsAssignment.getId();
                } catch (ApiException | TerracottaConnectorException e) {
                    log.error("Error updating obsolete assignment ID: [{}] in LMS context ID: [{}]", lmsAssignment.getId(), securedInfo.getLmsCourseId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();

            log.info("Checking Terracotta assignments for context ID: [{}] in LMS COMPLETE. Assignments marked as obsolete: [{}].",
                securedInfo.getContextId(),
                CollectionUtils.isNotEmpty(obsoleteAssignmentIds) ?
                    obsoleteAssignmentIds.stream()
                        .collect(Collectors.joining(", ")) :
                    "N/A"
        );
    }

    @Async
    @Override
    @Transactional(rollbackFor = { IOException.class })
    public void processAssignmentFileArchive(AssignmentFileArchive assignmentFileArchive) throws IOException {
        log.info("Processing assignment file archive with ID: [{}]", assignmentFileArchive.getUuid());
        // set file name; limit segments to 20 chars or less
        assignmentFileArchive.setFileName(
            String.format(
                "assignment_%s_--_files_(%s)",
                StringUtils.replace(assignmentFileArchive.getAssignmentTitle(), " ", "_"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH-mm").format(assignmentFileArchive.getCreatedAt())
            )
        );
        List<Treatment> treatments = treatmentRepository.findByAssignment_AssignmentIdOrderByCondition_ConditionIdAsc(assignmentFileArchive.getAssignmentId());
        List<Question> fileQuestions = new ArrayList<>();

        treatments.forEach(treatment -> {
            fileQuestions.addAll(treatment.getAssessment().getQuestions().stream()
                .filter(question -> QuestionTypes.FILE == question.getQuestionType())
                .toList()
            );
        });

        if (CollectionUtils.isEmpty(fileQuestions)) {
            log.info("No file questions found for assignment ID: [{}]. Aborting.", assignmentFileArchive.getAssignmentId());
            return;
        }

        // get all consenting participants for the experiment
        List<Participant> participants = participantRepository.findByExperiment_ExperimentId(assignmentFileArchive.getExperimentId()).stream()
            .filter(participant -> BooleanUtils.isTrue(participant.getConsent()))
            .toList();

        Map<Long, String> participantNameMap = new HashMap<>();

        for (Participant participant : participants) {
            String participantName = participant.getLtiUserEntity().getDisplayName();
            int index = 1;

            while (participantNameMap.containsValue(participantName)) {
                // handle duplicate participant names
                participantName = String.format("%s (%d)", participant.getLtiUserEntity().getDisplayName(), index++);
            }

            participantNameMap.put(participant.getParticipantId(), participantName);
        }

        // {particpantId: {questionId: file}}
        Map<String, Map<String, File>> userQuestionFiles = new HashMap<>();

        participantNameMap.entrySet().stream()
            .forEach(
                participant -> {
                    userQuestionFiles.put(participant.getValue(), new HashMap<>());
                }
            );

        fileQuestions.forEach(fileQuestion -> {
            List<AnswerFileSubmission> answerFileSubmissions = answerFileSubmissionRepository.findByQuestionSubmission_Question_QuestionId(fileQuestion.getQuestionId());

            answerFileSubmissions.stream()
                .forEach(
                    answerFileSubmission -> {
                        File file = fileStorageService.getFileSubmissionLocal(answerFileSubmission.getAnswerFileSubmissionId());

                        if (file != null) {
                            // rename file to actual file name
                            Path updatedPath = Path.of(
                                String.format(
                                    "%s/%s",
                                    StringUtils.substringBeforeLast(file.toPath().toString(), "/"),
                                    answerFileSubmission.getFileName()
                                )
                            );

                            try {
                                Path renamedFilePath = Files.move(file.toPath(), updatedPath, StandardCopyOption.REPLACE_EXISTING);

                                // get the user's file map
                                Map<String, File> userQuestionFileMap = userQuestionFiles.get(
                                    participantNameMap.get(
                                        answerFileSubmission.getQuestionSubmission().getSubmission().getParticipant().getParticipantId()
                                    )
                                );
                                // add the question and file mapping
                                userQuestionFileMap.put(StringUtils.substring(Jsoup.parse(fileQuestion.getHtml()).text(), 0, 20), renamedFilePath.toFile());
                            } catch (IOException e) {
                                log.error("Error renaming file: [{}] to: [{}]", file.toPath(), updatedPath, e);
                            }
                        }
                    }
                );
        });

        // create a directory for the assignment file archive
        Path parentPath = Paths.get(FileUtils.getTempDirectory().getAbsolutePath(), assignmentFileArchive.getUuid().toString());

        // process each user's files if files exist
        userQuestionFiles.entrySet().stream()
            .filter(userQuestionFile -> MapUtils.isNotEmpty(userQuestionFile.getValue()))
            .forEach(
                userQuestionFile -> {
                    try {
                        // create participant's directory
                        String participantDir = Files.createDirectories(
                            Path.of(
                                String.format(
                                    "%s/%s",
                                    parentPath,
                                    userQuestionFile.getKey()
                                )
                            )
                        )
                        .toFile()
                        .getAbsolutePath();

                        // copy each file to the user's question directory
                        userQuestionFile.getValue().entrySet().stream()
                            .forEach(
                                userQuestion -> {
                                    try {
                                        // create question directory in participant directory
                                        String participantQuestionDir = Files.createDirectories(Path.of(String.format("%s/%s", participantDir, userQuestion.getKey()))).toFile().getAbsolutePath();

                                        File file = userQuestion.getValue();
                                        Files.copy(
                                            file.toPath(),
                                            Paths.get(
                                                participantQuestionDir,
                                                file.getName()
                                            )
                                        );
                                    } catch (IOException e) {
                                        log.error("Error copying file for user ID: [{}] and question ID: [{}]", userQuestionFile.getKey(), userQuestion.getKey(), e);
                                    }
                                }
                            );
                    } catch (IOException e) {
                        log.error("Error copying file for user ID: [{}]", userQuestionFile.getKey(), e);
                    }
                }
            );

        // rename archive file to actual file name
        Path updatedParentPath = Path.of(
            String.format(
                "%s/%s",
                StringUtils.substringBeforeLast(parentPath.toString(), "/"),
                assignmentFileArchive.getFileName()
            )
        );

        try {
            Path renamedParentFilePath = Files.move(parentPath, updatedParentPath, StandardCopyOption.REPLACE_EXISTING);

            fileStorageService.compressDirectory(renamedParentFilePath.toString(), "", AssignmentFileArchive.COMPRESSED_FILE_EXTENSION, false);

            // create the compressed file and save archive .zip to file system
            File compressedFile = new File(String.format("%s%s", renamedParentFilePath, AssignmentFileArchive.COMPRESSED_FILE_EXTENSION));
            fileStorageService.saveAssignmentFileArchive(assignmentFileArchive, compressedFile);
            assignmentFileArchive.setStatus(AssignmentFileArchiveStatus.READY);

            // delete the original temp directory
            FileUtils.deleteQuietly(renamedParentFilePath.toFile());

            // delete the non-compressed file
            FileUtils.deleteQuietly(
                Path.of(
                    String.format(
                        "%s/%s",
                        assignmentFileArchiveLocalPathRoot,
                        assignmentFileArchive.getFileUri()
                    )
                )
                .toFile()
            );

            log.info("Processing assignment file archive with ID: [{}] COMPLETE", assignmentFileArchive.getUuid());
        } catch (IOException e) {
            log.error("Error renaming archive file: [{}] to: [{}]", parentPath, updatedParentPath, e);
            assignmentFileArchive.setStatus(AssignmentFileArchiveStatus.ERROR);
        }

        assignmentFileArchiveRepository.save(assignmentFileArchive);
    }

}
