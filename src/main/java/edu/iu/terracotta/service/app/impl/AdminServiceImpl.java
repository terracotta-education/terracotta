package edu.iu.terracotta.service.app.impl;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiMembershipRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.PlatformDeploymentRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.impl.ApiClientImpl;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.repository.AdminUserRepository;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ConsentDocumentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.AdminService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"unused", "PMD.GuardLogStatement"})
public class AdminServiceImpl implements AdminService {

    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ConsentDocumentRepository consentDocumentRepository;
    @Autowired private LtiMembershipRepository ltiMembershipRepository;
    @Autowired private PlatformDeploymentRepository platformDeploymentRepository;
    @Autowired private ApiClientImpl apiClient;

    @Override
    @Deprecated
    public void resyncTargetUris(long platformDeploymentId, String tokenOverride) throws ApiException, DataServiceException, ConnectionException, IOException {
        log.info("Starting assignment LTI Target Link URI update in the LMS for deployment ID: [{}]", platformDeploymentId);

        Optional<PlatformDeployment> platformDeployment = platformDeploymentRepository.findById(platformDeploymentId);

        if (platformDeployment.isEmpty()) {
            log.error("No platform deployment exists with ID: [{}]. Aborting.", platformDeploymentId);
            return;
        }

        // retrieve all instructors (role = 1) for this platform deployment
        List<String> instructorLmsIds = CollectionUtils.emptyIfNull(
                ltiMembershipRepository.findByRoleAndContext_ToolDeployment_PlatformDeployment_KeyId(1, platformDeploymentId)
            )
            .stream()
            .map(l -> l.getUser().getLmsUserId())
            .toList();

        if (CollectionUtils.isEmpty(instructorLmsIds)) {
            log.info("No instructors exist in Terracotta for deployment ID: [{}]. Aborting.", platformDeploymentId);
            return;
        }

        // retrieve all existing assignment IDs in Terracotta for this platform deployment
        List<String> assignmentIds = CollectionUtils.emptyIfNull(assignmentRepository.findAllByExposure_Experiment_PlatformDeployment_KeyId(platformDeploymentId)).stream()
            .map(Assignment::getLmsAssignmentId)
            .toList();

        // retrieve all existing consent assignment IDs in Terracotta for this platform deployment
        List<String> consentAssignmentIds = CollectionUtils.emptyIfNull(consentDocumentRepository.findAllByExperiment_PlatformDeployment_KeyId(platformDeploymentId)).stream()
            .map(ConsentDocument::getLmsAssignmentId)
            .toList();

        // create a list of both assignment and consent assignment IDs
        List<String> allAssignmentIds = (List<String>) CollectionUtils.union(assignmentIds, consentAssignmentIds);

        if (CollectionUtils.isEmpty(allAssignmentIds)) {
            log.info("No assignments found in terracotta for deployment ID: [{}], Aborting.", platformDeploymentId);
            return;
        }

        instructorLmsIds.forEach(
            instructorLmsId -> {
                try {
                    // retrieve courses for the instructor
                    List<LmsCourse> lmsCourses = apiClient.listCoursesForUser(platformDeployment.get(), instructorLmsId, tokenOverride);

                    if (CollectionUtils.isEmpty(lmsCourses)) {
                        log.info("No courses exist in the LMS for instructor ID: [{}]", instructorLmsId);
                        return;
                    }

                    lmsCourses.stream()
                        .forEach(
                            lmsCourse -> {
                                try {
                                    apiClient.resyncAssignmentTargetUrisInLms(platformDeployment.get(), null, platformDeploymentId, tokenOverride, assignmentIds, consentAssignmentIds, allAssignmentIds);
                                } catch (ApiException | TerracottaConnectorException e) {
                                    log.info("An error occurred updating assignments for deployment ID: [{}] in the LMS. Error: [{}]", platformDeploymentId, e.getMessage(), e);
                                }
                            }
                        );
                } catch (Exception e) {
                    log.info("An error occurred updating assignments for deployment ID: [{}] in the LMS. Error: [{}]", platformDeploymentId, e.getMessage(), e);
                }
            }
        );

        log.info("Assignment LTI Target Link URI update for deployment ID: [{}] COMPLETE!", platformDeploymentId);
    }

    @Override
    public boolean isTerracottaAdmin(String userKey) {
        return adminUserRepository.existsByLtiUserEntity_UserKeyAndEnabledTrue(userKey);
    }

}
