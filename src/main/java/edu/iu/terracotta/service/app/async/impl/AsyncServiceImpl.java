package edu.iu.terracotta.service.app.async.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.ObsoleteAssignment;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;
import edu.iu.terracotta.dao.repository.ObsoleteAssignmentRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.async.AsyncService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AsyncServiceImpl implements AsyncService {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ExperimentRepository experimentRepository;
    @Autowired private LtiContextRepository ltiContextRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private ObsoleteAssignmentRepository obsoleteAssignmentRepository;
    @Autowired private AssignmentService assignmentService;
    @Autowired private ApiClient apiClient;

    @PersistenceContext private EntityManager entityManager;

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

}
