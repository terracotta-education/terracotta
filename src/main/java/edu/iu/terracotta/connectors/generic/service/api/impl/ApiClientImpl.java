package edu.iu.terracotta.connectors.generic.service.api.impl;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Submission;

@Primary
@Service
public class ApiClientImpl implements ApiClient {

    @Autowired private ConnectorService<ApiClient> connectorService;

    private ApiClient instance(LtiUserEntity apiUser) throws TerracottaConnectorException {
        return instance(apiUser.getPlatformDeployment());
    }

    private ApiClient instance(Assignment assignment) throws TerracottaConnectorException {
        return instance(assignment.getExposure().getExperiment().getPlatformDeployment());
    }

    private ApiClient instance(Submission submission) throws TerracottaConnectorException {
        return instance(submission.getAssessment().getTreatment().getCondition().getExperiment().getPlatformDeployment());
    }

    private ApiClient instance(PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return connectorService.instance(platformDeployment, ApiClient.class);
    }

    @Override
    public LmsAssignment createLmsAssignment(LtiUserEntity apiUser, Assignment assignment, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        return instance(apiUser).createLmsAssignment(apiUser, assignment, lmsCourseId);
    }

    @Override
    public List<LmsAssignment> listAssignments(LtiUserEntity apiUser, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        return instance(apiUser).listAssignments(apiUser, lmsCourseId);
    }

    @Override
    public List<LmsAssignment> listAssignments(PlatformDeployment platformDeployment, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return instance(platformDeployment).listAssignments(platformDeployment, lmsCourseId, tokenOverride);
    }

    @Override
    public Optional<LmsAssignment> checkAssignmentExists(LtiUserEntity apiUser, String lmsAssignmentId, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        return instance(apiUser).checkAssignmentExists(apiUser, lmsAssignmentId, lmsCourseId);
    }

    @Override
    public Optional<LmsAssignment> listAssignment(LtiUserEntity apiUser, String lmsCourseId, String lmsAssignmentId) throws ApiException, TerracottaConnectorException {
        return instance(apiUser).listAssignment(apiUser, lmsCourseId, lmsAssignmentId);
    }

    public Optional<LmsAssignment> editAssignment(LtiUserEntity apiUser, LmsAssignment lmsAssignment, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        return instance(apiUser).editAssignment(apiUser, lmsAssignment, lmsCourseId);
    }

    @Override
    public Optional<LmsAssignment> editAssignment(PlatformDeployment platformDeployment, LmsAssignment lmsAssignment, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return instance(platformDeployment).editAssignment(platformDeployment, lmsAssignment, lmsCourseId, tokenOverride);
    }

    @Override
    public LmsAssignment restoreAssignment(Assignment assignment) throws ApiException, IOException, TerracottaConnectorException {
        return instance(assignment).restoreAssignment(assignment);
    }

    @Override
    public void editAssignmentNameInLms(Assignment assignment, String lmsCourseId, String newName, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException {
        instance(assignment).editAssignmentNameInLms(assignment, lmsCourseId, newName, instructorUser);
    }

    @Override
    public void deleteAssignmentInLms(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException {
        instance(assignment).deleteAssignmentInLms(assignment, lmsCourseId, instructorUser);
    }

    @Override
    public void deleteAssignmentInLms(LmsAssignment lmsAssignment, String lmsCourseId, LtiUserEntity apiUser) throws ApiException, IOException, TerracottaConnectorException {
        instance(apiUser).deleteAssignmentInLms(lmsAssignment, lmsCourseId, apiUser);
    }

    @Override
    public LmsAssignment uploadConsentFile(Experiment experiment, ConsentDocument consentDocument, LtiUserEntity apiUser) throws ApiException, IOException, TerracottaConnectorException {
        return instance(apiUser).uploadConsentFile(experiment, consentDocument, apiUser);
    }

    @Override
    public void resyncAssignmentTargetUrisInLms(PlatformDeployment platformDeployment, LtiUserEntity ltiUserEntity, long lmsCourseId, String tokenOverride, List<String> lmsAssignmentIds, List<String> consentLmsAssignmentIds, List<String> allLmsAssignmentIds)
        throws ApiException, TerracottaConnectorException {
        instance(platformDeployment).resyncAssignmentTargetUrisInLms(platformDeployment, ltiUserEntity, lmsCourseId, tokenOverride, lmsAssignmentIds, consentLmsAssignmentIds, allLmsAssignmentIds);
    }

    @Override
    public List<LmsCourse> listCoursesForUser(PlatformDeployment platformDeployment, String lmsUserId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return instance(platformDeployment).listCoursesForUser(platformDeployment, lmsUserId, tokenOverride);
    }

    @Override
    public Optional<LmsCourse> editCourse(PlatformDeployment platformDeployment, LmsCourse lmsCourse, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return instance(platformDeployment).editCourse(platformDeployment, lmsCourse, lmsCourseId, tokenOverride);
    }

    @Override
    public void addLmsExtensions(Score score, Submission submission, boolean studentSubmission) throws ApiException, IOException, TerracottaConnectorException {
        instance(submission).addLmsExtensions(score, submission, studentSubmission);
    }

    @Override
    public List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, String lmsAssignmentId, String lmsCourseId) throws ApiException, IOException, TerracottaConnectorException {
        return instance(apiUser).listSubmissions(apiUser, lmsAssignmentId, lmsCourseId);
    }

}
