package edu.iu.terracotta.connectors.brightspace.service.lti.advantage.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiContextEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.membership.CourseUsers;
import edu.iu.terracotta.connectors.generic.dao.model.lti.LtiToken;
import edu.iu.terracotta.connectors.generic.dao.model.lti.Roles;
import edu.iu.terracotta.connectors.generic.dao.model.lti.enums.LtiAgsScope;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ConnectionException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.exceptions.helper.ExceptionMessageGenerator;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageConnectorHelper;
import edu.iu.terracotta.connectors.generic.service.lti.advantage.AdvantageMembershipService;
import edu.iu.terracotta.service.app.async.LmsUserBatchAsyncService;
import edu.iu.terracotta.utils.TextConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@TerracottaConnector(LmsConnector.BRIGHTSPACE)
@SuppressWarnings({"rawtypes", "PMD.GuardLogStatement"})
public class BrightspaceAdvantageMembershipServiceImpl implements AdvantageMembershipService {

    private final LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository;
    private final LmsUserBatchRepository lmsUserBatchRepository;
    private final AdvantageConnectorHelper advantageConnectorHelper;
    private final ExceptionMessageGenerator exceptionMessageGenerator;
    private final LmsUserBatchAsyncService lmsUserBatchAsyncService;

    @PersistenceContext private EntityManager entityManager;

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

            LmsUserBatchProcessing lmsUserBatchProcessing = lmsUserBatchProcessingRepository.saveAndFlush(
                LmsUserBatchProcessing.builder()
                    .batchId(batchId)
                    .status(LmsUserBatchStatus.IN_PROGRESS)
                    .build()
            );

            if (!membershipGetResponse.getStatusCode().is2xxSuccessful()) {
                String errorMessage = String.format("Can't get the membership for context ID: [%s]", context.getContextId());
                log.error(errorMessage);

                lmsUserBatchProcessing = lmsUserBatchProcessingRepository.findByBatchId(batchId)
                    .orElseGet(() -> LmsUserBatchProcessing.builder().batchId(batchId).build());
                lmsUserBatchProcessing.setStatus(LmsUserBatchStatus.FAILED);
                lmsUserBatchProcessing.setMessage(errorMessage);
                lmsUserBatchProcessingRepository.saveAndFlush(lmsUserBatchProcessing);

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
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    /**
     * Add student role users to the batch data database
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

        lmsUserBatchRepository.saveAll(usersToSave);

        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional
    public List<LmsUserBatch> getAllLmsUsers(LtiToken ltiToken, LtiContextEntity context) throws ConnectionException, TerracottaConnectorException {
        UUID batchId = UUID.randomUUID();
        callMembershipService(ltiToken, context, batchId, false);
        lmsUserBatchAsyncService.success(batchId);

        return lmsUserBatchRepository.findByBatchId(batchId, Pageable.unpaged());
    }

}
