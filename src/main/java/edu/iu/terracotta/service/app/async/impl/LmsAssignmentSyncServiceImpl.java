package edu.iu.terracotta.service.app.async.impl;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.service.api.ApiJwtService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.async.AssignmentAsyncService;
import edu.iu.terracotta.service.app.async.ExperimentCopyRecreationAsyncService;
import edu.iu.terracotta.service.app.async.LmsAssignmentSyncService;
import edu.iu.terracotta.service.app.distribute.ExperimentCopyCandidateService;
import edu.iu.terracotta.utils.LmsAuthorizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.GuardLogStatement")
public class LmsAssignmentSyncServiceImpl implements LmsAssignmentSyncService {

    private final ApiJwtService apiJwtService;
    private final AssignmentService assignmentService;
    private final AssignmentAsyncService assignmentAsyncService;
    private final ExperimentCopyCandidateService experimentCopyCandidateService;
    private final ExperimentCopyRecreationAsyncService experimentCopyRecreationAsyncService;

    @Override
    public void sync(SecuredInfo securedInfo) {
        try {
            List<LmsAssignment> lmsAssignments = assignmentService.getAllAssignmentsForLmsCourse(securedInfo);

            // only an instructor can recreate experiments: it runs on their LMS access
            if (apiJwtService.isInstructorOrHigher(securedInfo) && experimentCopyCandidateService.stageFromCopiedAssignments(securedInfo, lmsAssignments)) {
                log.info("Recreating experiments copied into context ID: [{}] as the launching instructor; the rest of the LMS assignment sync waits until they're recreated", securedInfo.getContextId());
                experimentCopyRecreationAsyncService.recreate(securedInfo.getContextId(), securedInfo.getUserId());
                return;
            }

            // deepcode ignore Ssrf: LMS calls go to the stored PlatformDeployment base URL for the launch's own context
            assignmentAsyncService.handleAssignmentTasksInLmsByContext(securedInfo, lmsAssignments);
        } catch (ApiException e) {
            if (LmsAuthorizationUtils.isAuthorizationFailure(e)) {
                // the user hasn't authorized Terracotta's LMS API access (yet) - the launch asks
                // them to, and the sync runs on a later launch (see AssignmentAsyncServiceImpl)
                log.warn("Skipping LMS assignment sync for context ID: [{}] - user does not yet have an LMS API token", securedInfo.getContextId());
                return;
            }

            log.error("Error syncing LMS assignments for context ID: [{}]", securedInfo.getContextId(), e);
        } catch (Exception e) {
            log.error("Error syncing LMS assignments for context ID: [{}]", securedInfo.getContextId(), e);
        }
    }

    @Async
    @Override
    public void syncInBackground(SecuredInfo securedInfo) {
        sync(securedInfo);
    }

}
