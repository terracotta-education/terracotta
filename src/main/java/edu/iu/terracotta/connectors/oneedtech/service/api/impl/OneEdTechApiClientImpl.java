package edu.iu.terracotta.connectors.oneedtech.service.api.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsCourse;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsFile;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsSubmission;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsCreateConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetSingleConversationOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lms.options.LmsGetUsersInCourseOptions;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.LineItem;
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.api.ApiClient;
import edu.iu.terracotta.connectors.oneedtech.dao.model.extended.AssignmentExtended;
import edu.iu.terracotta.connectors.oneedtech.dao.model.extended.CourseExtended;
import edu.iu.terracotta.connectors.oneedtech.dao.model.extended.SubmissionExtended;
import edu.iu.terracotta.connectors.oneedtech.service.lti.advantage.impl.OneEdTechAdvantageAgsServiceImpl;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Submission;

@Service
@TerracottaConnector(LmsConnector.ONE_ED_TECH)
@SuppressWarnings({"PMD.UncommentedEmptyMethodBody"})
public class OneEdTechApiClientImpl implements ApiClient {

    @Autowired private OneEdTechAdvantageAgsServiceImpl oneEdTechAdvantageAgsService;

    @Override
    public AssignmentExtended createLmsAssignment(LtiUserEntity apiUser, Assignment assignment, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        LineItem response = null;

        try {
            LtiToken ltiToken = oneEdTechAdvantageAgsService.getToken(null, apiUser.getPlatformDeployment());
            LineItem lineItem = LineItem.builder()
                .label(assignment.getTitle())
                .scoreMaximum(100F)
                .build();
            response = oneEdTechAdvantageAgsService.postLineItem(ltiToken, assignment.getExposure().getExperiment().getLtiContextEntity(), lineItem);
        } catch (ConnectionException e) {
            throw new TerracottaConnectorException(String.format("Error creating line item for LMS course ID: [%s] and assignment ID: [%s]", lmsCourseId, assignment.getAssignmentId()), e);
        }

        return AssignmentExtended.builder()
            .assignment(
                edu.iu.terracotta.connectors.oneedtech.dao.model.lms.Assignment.builder()
                    .id(response.getId())
                    .label(response.getLabel())
                    .resourceLinkId(response.getId())
                    .scoreMaximum(response.getScoreMaximum())
                    .build()
            )
            .build();
    }

    @Override
    public List<LmsAssignment> listAssignments(LtiUserEntity apiUser, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        return Collections.singletonList(AssignmentExtended.builder().build());
    }

    @Override
    public List<LmsAssignment> listAssignments(PlatformDeployment platformDeployment, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return Collections.singletonList(AssignmentExtended.builder().build());
    }

    @Override
    public Optional<LmsAssignment> checkAssignmentExists(LtiUserEntity apiUser, String assignmentId, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        return Optional.of(AssignmentExtended.builder().build());
    }

    @Override
    public Optional<LmsAssignment> listAssignment(LtiUserEntity apiUser, String lmsCourseId, String assignmentId) throws ApiException, TerracottaConnectorException {
        return Optional.of(AssignmentExtended.builder().build());
    }

    @Override
    public Optional<LmsAssignment> editAssignment(LtiUserEntity apiUser, LmsAssignment lmsAssignment, String lmsCourseId) throws ApiException, TerracottaConnectorException {
        return Optional.of(AssignmentExtended.builder().build());
    }

    @Override
    public Optional<LmsAssignment> editAssignment(PlatformDeployment platformDeployment, LmsAssignment lmsAssignment, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return Optional.of(AssignmentExtended.builder().build());
    }

    @Override
    public AssignmentExtended restoreAssignment(Assignment assignment) throws ApiException, IOException, TerracottaConnectorException {
        return AssignmentExtended.builder().build();
    }

    @Override
    public void editAssignmentNameInLms(Assignment assignment, String lmsCourseId, String newName, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException {
    }

    @Override
    public void deleteAssignmentInLms(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException {
    }

    @Override
    public void deleteAssignmentInLms(LmsAssignment lmsAssignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException {
    }

    @Override
    public AssignmentExtended uploadConsentFile(Experiment experiment, ConsentDocument consentDocument, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException {
        return AssignmentExtended.builder().build();
    }

    @Override
    public void resyncAssignmentTargetUrisInLms(PlatformDeployment platformDeployment, LtiUserEntity ltiUserEntity, long lmsCourseId, String tokenOverride, List<String> assignmentIds, List<String> consentAssignmentIds, List<String> allAssignmentIds)
        throws ApiException, TerracottaConnectorException {
    }

    @Override
    public List<LmsCourse> listCoursesForUser(PlatformDeployment platformDeployment, String lmsUserId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return Collections.singletonList(CourseExtended.builder().build());
    }

    @Override
    public Optional<LmsCourse> editCourse(PlatformDeployment platformDeployment, LmsCourse lmsCourse, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException {
        return Optional.of(CourseExtended.builder().build());
    }

    @Override
    public List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, String assignmentId, String lmsCourseId) throws ApiException, IOException, TerracottaConnectorException {
        return Collections.singletonList(SubmissionExtended.builder().build());
    }

    @Override
    public List<LmsSubmission> listSubmissionsForMultipleAssignments(LtiUserEntity apiUser, String lmsCourseId, List<String> lmsAssignmentIds) throws ApiException, IOException, TerracottaConnectorException {
        throw new UnsupportedOperationException("Unimplemented method 'listSubmissionsForMultipleAssignments'");
    }

    @Override
    public void addLmsExtensions(Score score, Submission submission, boolean studentSubmission) throws ApiException, IOException, TerracottaConnectorException {
    }

    @Override
    public List<LmsConversation> sendConversation(LmsCreateConversationOptions lmsCreateConversationOptions, LtiUserEntity apiUser) throws ApiException {
        throw new UnsupportedOperationException("Unimplemented method 'sendConversation'");
    }

    @Override
    public Optional<LmsConversation> getConversation(LmsGetSingleConversationOptions lmsGetSingleConversationOptions, LtiUserEntity apiUser) throws ApiException {
        throw new UnsupportedOperationException("Unimplemented method 'getConversation'");
    }

    @Override
    public List<LmsUser> listUsersForCourse(LmsGetUsersInCourseOptions lmsGetUsersInCourseOptions, LtiUserEntity apiUser) throws ApiException {
        throw new UnsupportedOperationException("Unimplemented method 'listUsersForCourse'");
    }

    @Override
    public Optional<LmsFile> getFile(LtiUserEntity apiUser, String lmsFileId) throws ApiException {
        throw new UnsupportedOperationException("Unimplemented method 'getFile'");
    }

    @Override
    public List<LmsFile> getFiles(LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException {
        throw new UnsupportedOperationException("Unimplemented method 'getFiles'");
    }

}
