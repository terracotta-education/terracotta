package edu.iu.terracotta.connectors.generic.service.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
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
import edu.iu.terracotta.connectors.generic.dao.model.lti.ags.Score;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.ConsentDocument;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Outcome;
import edu.iu.terracotta.dao.entity.Submission;

@TerracottaConnector(LmsConnector.GENERIC)
public interface ApiClient {

    LmsAssignment createLmsAssignment(LtiUserEntity apiUser, Assignment assignment, String lmsCourseId) throws ApiException, TerracottaConnectorException;
    List<LmsAssignment>listAssignments(LtiUserEntity apiUser, LtiContextEntity ltiContext) throws ApiException, TerracottaConnectorException;
    List<LmsAssignment>listAssignments(LtiUserEntity apiUser, Experiment experiment) throws ApiException, TerracottaConnectorException;
    List<LmsAssignment>listAssignments(PlatformDeployment platformDeployment, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException;
    Optional<LmsAssignment>checkAssignmentExists(LtiUserEntity apiUser, String lmsAssignmentId, String lmsCourseId) throws ApiException, TerracottaConnectorException;
    Optional<LmsAssignment>listAssignment(LtiUserEntity apiUser, String lmsCourseId, String lmsAssignmentId) throws ApiException, TerracottaConnectorException;
    Optional<LmsAssignment>listAssignment(LtiUserEntity apiUser, String lmsCourseId, Assignment assignment) throws ApiException, TerracottaConnectorException;
    Optional<LmsAssignment>editAssignment(LtiUserEntity apiUser, LmsAssignment lmsAssignment, String lmsCourseId) throws ApiException, TerracottaConnectorException;
    Optional<LmsAssignment>editAssignment(PlatformDeployment platformDeployment, LmsAssignment lmsAssignment, String lmsCourseId, String tokenOverride) throws ApiException, TerracottaConnectorException;
    LmsAssignment restoreAssignment(Assignment assignment) throws ApiException, IOException, TerracottaConnectorException;
    void editAssignmentNameInLms(Assignment assignment, String lmsCourseId, String newName, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException;
    void deleteAssignmentInLms(Assignment assignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException;
    void deleteAssignmentInLms(LmsAssignment lmsAssignment, String lmsCourseId, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException;
    LmsAssignment uploadConsentFile(Experiment experiment, ConsentDocument consentDocument, LtiUserEntity instructorUser) throws ApiException, IOException, TerracottaConnectorException;
    void resyncAssignmentTargetUrisInLms(PlatformDeployment platformDeployment, LtiUserEntity ltiUserEntity, long lmsCourseId, String tokenOverride, List<String> lmsAssignmentIds, List<String> consentLmsAssignmentIds, List<String> allLmsAssignmentIds)
        throws ApiException, TerracottaConnectorException;
    void updateAssignmentMetadata(Assignment assignment, LmsAssignment lmsAssignment) throws TerracottaConnectorException;

    List<LmsCourse> listCoursesForUser(PlatformDeployment platformDeployment, String lmsUserId, String tokenOverride) throws ApiException, TerracottaConnectorException;

    List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, Outcome outcome, String lmsCourseId) throws ApiException, IOException, TerracottaConnectorException;
    List<LmsSubmission> listSubmissions(LtiUserEntity apiUser, String lmsAssignmentId, String lmsCourseId) throws ApiException, IOException, TerracottaConnectorException;
    List<LmsSubmission> listSubmissionsForMultipleAssignments(LtiUserEntity apiUser, String lmsCourseId, List<String> lmsAssignmentIds) throws ApiException, IOException, TerracottaConnectorException;
    void addLmsExtensions(Score score, Submission submission, boolean studentSubmission) throws ApiException, IOException, TerracottaConnectorException;

    List<LmsConversation> sendConversation(LmsCreateConversationOptions lmsConversationOptions, LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException;
    Optional<LmsConversation> getConversation(LmsGetSingleConversationOptions lmsGetSingleConversationOptions, LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException;
    List<LmsUser> listUsersForCourse(LmsGetUsersInCourseOptions lmsGetUsersInCourseOptions, LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException;
    Optional<LmsFile> getFile(LtiUserEntity apiUser, String lmsFileId) throws ApiException, TerracottaConnectorException;
    List<LmsFile> getFiles(LtiUserEntity apiUser) throws ApiException, TerracottaConnectorException;

}
