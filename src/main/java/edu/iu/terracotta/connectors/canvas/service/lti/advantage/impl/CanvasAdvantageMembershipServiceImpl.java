package edu.iu.terracotta.connectors.canvas.service.lti.advantage.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageConnectorHelper;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageMembershipService;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import edu.iu.terracotta.utils.TextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@TerracottaConnector(LmsConnector.CANVAS)
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class CanvasAdvantageMembershipServiceImpl implements AdvantageMembershipService {

    private final LmsUserBatchRepository lmsUserBatchRepository;
    private final LmsUserBatchWriteService lmsUserBatchWriteService;
    private final AdvantageConnectorHelper advantageConnectorHelper;
    private final ExceptionMessageGenerator exceptionMessageGenerator;
    private final LmsUserBatchAsyncService lmsUserBatchAsyncService;

    @Value("${app.participant.batch.size:500}")
    private int batchSize;

    @Value("${app.token.logging.enabled:true}")
    private boolean tokenLoggingEnabled;

    @Override
    public LtiToken getToken(PlatformDeployment platformDeployment) throws ConnectionException, TerracottaConnectorException {
        return advantageConnectorHelper.getToken(platformDeployment, LtiAgsScope.NRPS_MEMBERSHIP_READONLY.key());
    }

    @Override
    @Transactional
    public CourseUsers callMembershipService(LtiToken ltiToken, LtiContextEntity context, UUID batchId) throws ConnectionException, TerracottaConnectorException {
        return callMembershipService(ltiToken, context, batchId, true);
    }

    @Override
    @Transactional
    public CourseUsers callMembershipService(LtiToken ltiToken, LtiContextEntity context, UUID batchId, boolean onlyStudents) throws ConnectionException, TerracottaConnectorException {
        if (tokenLoggingEnabled) {
            log.debug("{}{}", TextConstants.TOKEN, ltiToken.getAccess_token());
        }

        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntityWithAccept(ltiToken, LtiAgsScope.NRPS_MEMBERSHIP_JSON_ACCEPT.key());
            String url = String.format("%s?limit=%s", context.getContext_memberships_url(), batchSize);
            ResponseEntity<CourseUsers> membershipGetResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                CourseUsers.class
            );

            lmsUserBatchWriteService.startBatch(batchId, context.getContextId());

            if (!membershipGetResponse.getStatusCode().is2xxSuccessful()) {
                String errorMessage = String.format("Can't get the membership for context ID: [%s]", context.getContextId());
                log.error(errorMessage);

                // caught below, which marks the batch failed - no need to do it here too
                throw new ConnectionException(errorMessage);
            }

            if (Objects.isNull(membershipGetResponse.getBody())) {
                return null;
            }

            addToBatchData(batchId, membershipGetResponse.getBody().getCourseUserList(), onlyStudents);
            String nextPage = advantageConnectorHelper.nextPage(membershipGetResponse.getHeaders());

            while (nextPage != null) {
                ResponseEntity<CourseUsers> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET, request, CourseUsers.class);

                if (Objects.isNull(responseForNextPage.getBody())) {
                    return null;
                }

                addToBatchData(batchId, responseForNextPage.getBody().getCourseUserList(), onlyStudents);
                nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
            }

            return null;
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the membership");
            log.error(exceptionMsg.toString(), e);
            lmsUserBatchWriteService.markFailed(batchId, e.getMessage());
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    /**
     * Add student role users to the batch data database. Saved via lmsUserBatchWriteService,
     * which commits each page independently of this method's own transaction - a huge course
     * roster can take many pages/minutes to fully paginate, and holding every page's inserts
     * uncommitted for that whole duration is what caused concurrent syncs for OTHER contexts to
     * hit "Lock wait timeout exceeded" on this same table.
     *
     * @param batchId
     * @param courseUsers
     */
    private void addToBatchData(UUID batchId, List<CourseUser> courseUsers, boolean onlyStudents) {
        List<LmsUserBatch> usersToSave = courseUsers.stream()
            .filter(courseUser -> !onlyStudents || courseUser.getRoles().contains(Roles.LEARNER) || courseUser.getRoles().contains(Roles.MEMBERSHIP_LEARNER))
            .map(courseUser ->
                LmsUserBatch.builder()
                    .batchId(batchId)
                    .email(courseUser.getEmail())
                    .lmsUserId(String.valueOf(courseUser.getUserId()))
                    .name(courseUser.getName())
                    .userKey(courseUser.getUserId())
                    .build()
            )
            .toList();

        lmsUserBatchWriteService.saveUsers(usersToSave);
    }

    // READ_COMMITTED (rather than MySQL's default REPEATABLE READ): callMembershipService below
    // writes lms_user_batch via LmsUserBatchWriteService.startBatch/saveUsers in REQUIRES_NEW
    // sub-transactions, which commit independently and before this method's own later plain read
    // of that same table (findByBatchId). Under REPEATABLE READ, this transaction's
    // consistent-read snapshot is fixed as of its first query, so that later read would still see
    // the pre-sync (empty) state despite the sub-transactions having already committed - see the
    // identical fix on ParticipantAsyncServiceImpl.updateParticipantData.
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<LmsUserBatch> getAllLmsUsers(LtiToken ltiToken, LtiContextEntity context) throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();
        callMembershipService(ltiToken, context, batchId, false);
        lmsUserBatchAsyncService.success(batchId);

        return lmsUserBatchRepository.findByBatchId(batchId, Pageable.unpaged());
    }

}
