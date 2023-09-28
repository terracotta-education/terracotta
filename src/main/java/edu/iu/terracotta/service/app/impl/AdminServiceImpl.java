package edu.iu.terracotta.service.app.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.model.app.ConsentDocument;
import edu.iu.terracotta.model.canvas.AssignmentExtended;
import edu.iu.terracotta.model.canvas.CourseExtended;
import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.service.app.AdminService;
import edu.iu.terracotta.service.canvas.CanvasAPIClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AdminServiceImpl implements AdminService {

    @Autowired private AllRepositories allRepositories;
    @Autowired private CanvasAPIClient canvasAPIClient;

    @Override
    public void resyncTargetUris(long platformDeploymentId, String tokenOverride) throws CanvasApiException, DataServiceException, ConnectionException, IOException {
        log.info("Starting assignment LTI Target Link URI update in Canvas for deployment ID: '{}'", platformDeploymentId);

        Optional<PlatformDeployment> platformDeployment = allRepositories.platformDeploymentRepository.findById(platformDeploymentId);

        if (!platformDeployment.isPresent()) {
            log.error("No platform deployment exists with ID: '{}'. Aborting.", platformDeploymentId);
            return;
        }

        // retrieve all instructors (role = 1) for this platform deployment
        List<String> instructorLmsIds = CollectionUtils.emptyIfNull(
                allRepositories.ltiMembershipRepository.findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(1, platformDeploymentId)
            )
            .stream()
            .map(l -> l.getUser().getLmsUserId())
            .toList();

        if (CollectionUtils.isEmpty(instructorLmsIds)) {
            log.info("No instructors exist in Terracotta for deployment ID: '{}'. Aborting.", platformDeploymentId);
            return;
        }

        // retrieve all existing assignment IDs in Terracotta for this platform deployment
        List<String> assignmentIds = CollectionUtils.emptyIfNull(allRepositories.assignmentRepository.findAllByExposure_Experiment_PlatformDeployment_KeyId(platformDeploymentId)).stream()
            .map(Assignment::getLmsAssignmentId)
            .toList();

        // retrieve all existing consent assignment IDs in Terracotta for this platform deployment
        List<String> consentAssignmentIds = CollectionUtils.emptyIfNull(allRepositories.consentDocumentRepository.findAllByExperiment_PlatformDeployment_KeyId(platformDeploymentId)).stream()
            .map(ConsentDocument::getLmsAssignmentId)
            .toList();

        // create a list of both assignment and consent assignment IDs
        List<String> allAssignmentIds = (List<String>) CollectionUtils.union(assignmentIds, consentAssignmentIds);

        if (CollectionUtils.isEmpty(allAssignmentIds)) {
            log.info("No assignments found in terracotta for deployment ID: '{}', Aborting.", platformDeploymentId);
            return;
        }

        List<Integer> canvasCoursesCompleted = new ArrayList<>();

        instructorLmsIds.forEach(
            instructorLmsId -> {
                try {
                    // retrieve courses for the instructor
                    List<CourseExtended> canvasCourses = canvasAPIClient.listCoursesForUser(platformDeployment.get().getBaseUrl(), instructorLmsId, tokenOverride);

                    if (CollectionUtils.isEmpty(canvasCourses)) {
                        log.info("No courses exist in Canvas for instructor ID: '{}'", instructorLmsId);
                        return;
                    }

                    canvasCourses.stream()
                        .filter(canvasCourse -> !CollectionUtils.containsAny(canvasCoursesCompleted, canvasCourse.getId()))
                        .forEach(
                            canvasCourse -> {
                                List<AssignmentExtended> canvasAssignments;

                                try {
                                    // retrieve assignments for this course in Canvas
                                    canvasAssignments = canvasAPIClient.listAssignments(platformDeployment.get().getBaseUrl(), Integer.toString(canvasCourse.getId()), tokenOverride);
                                } catch (CanvasApiException e) {
                                    log.info("An error occurred updating assignments for Canvas course ID: '{}' for deployment ID: '{}'. Error: '{}'", canvasCourse.getId(), platformDeploymentId, e.getMessage());
                                    return;
                                }

                                if (CollectionUtils.isEmpty(canvasAssignments)) {
                                    log.info("No assignments exist in Canvas for course ID: '{}.", canvasCourse.getId());
                                    return;
                                }

                                List<Integer> assignmentsUpdatedTargetLink = canvasAssignments.stream()
                                    .filter(canvasAssignment -> allAssignmentIds.contains(Integer.toString(canvasAssignment.getId())))
                                    .filter(
                                        canvasAssignment -> {
                                            String[] baseUrl = StringUtils.splitByWholeSeparator(canvasAssignment.getExternalToolTagAttributes().getUrl(), "/lti3");

                                            if (ArrayUtils.isEmpty(baseUrl)) {
                                                return false;
                                            }

                                            if (StringUtils.equalsIgnoreCase(baseUrl[0], platformDeployment.get().getLocalUrl())) {
                                                return false;
                                            }

                                            return true;
                                        }
                                    )
                                    .map(
                                        assignmentToUpdate -> {
                                            String updatedTargetLinkUri = StringUtils.replaceOnce(
                                                assignmentToUpdate.getExternalToolTagAttributes().getUrl(),
                                                StringUtils.splitByWholeSeparator(assignmentToUpdate.getExternalToolTagAttributes().getUrl(), "/lti3")[0],
                                                platformDeployment.get().getLocalUrl()
                                            );

                                            log.info("Updating assignment ID: '{}' LTI Target Link URI in Canvas: from '{} to '{}'",
                                                assignmentToUpdate.getId(),
                                                assignmentToUpdate.getExternalToolTagAttributes().getUrl(),
                                                updatedTargetLinkUri
                                            );

                                            try {
                                                assignmentToUpdate.getExternalToolTagAttributes().setUrl(updatedTargetLinkUri);
                                                canvasAPIClient.editAssignment(platformDeployment.get().getBaseUrl(), assignmentToUpdate, assignmentToUpdate.getCourseId(), tokenOverride);
                                            } catch (CanvasApiException e) {
                                                log.error("Error updating LTI Target Link URIs in Canvas. Assignment ID: '{}'. Error: '{}'", assignmentToUpdate.getId(), e.getMessage());
                                            }

                                            canvasCoursesCompleted.add(canvasCourse.getId());

                                            return assignmentToUpdate.getId();
                                        }
                                    )
                                    .toList();

                                log.info("Updating Assignment Target Link URIs for Canvas course ID: '{}' for deployment ID: '{}' in Canvas COMPLETE. Updated: {}",
                                    canvasCourse.getId(),
                                    platformDeploymentId,
                                    CollectionUtils.isNotEmpty(assignmentsUpdatedTargetLink) ?
                                        assignmentsUpdatedTargetLink.stream()
                                            .map(l -> Integer.toString(l))
                                            .collect(Collectors.joining(", ")) :
                                        "N/A"
                                );
                            }
                        );
                } catch (Exception e) {
                    log.info("An error occurred updating assignments for deployment ID: '{}' in Canvas. Error: '{}'", platformDeploymentId, e.getMessage(), e);
                    return;
                }
            }
        );

        log.info("Assignment LTI Target Link URI update for deployment ID: '{}' COMPLETE!", platformDeploymentId);
    }

    @Override
    public boolean isTerracottaAdmin(String userKey) {
        if (StringUtils.isEmpty(userKey)) {
            return false;
        }

        return allRepositories.adminUserRepository.existsByLtiUserEntity_UserKeyAndEnabledTrue(userKey);
    }

}
