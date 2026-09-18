package edu.iu.terracotta.service.app.integrations.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.integrations.IntegrationToken;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.dao.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.dao.model.enums.QuestionTypes;
import edu.iu.terracotta.dao.repository.integrations.IntegrationTokenRepository;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.integrations.IntegrationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationTokenServiceImpl implements IntegrationTokenService {

    private final IntegrationTokenRepository integrationTokenRepository;

    @Value("${app.integrations.token.ttl:43200}")
    private long ttl;

    @Override
    public void create(Submission submission, SecuredInfo securedInfo) throws IntegrationTokenNotFoundException {
        if (!submission.isIntegration()) {
            // not an integration; no token needed
            return;
        }

        if (!submission.getAssessment().getQuestions().stream().anyMatch(question -> question.getQuestionType() == QuestionTypes.INTEGRATION)) {
            // no integration type questions; no token needed
            return;
        }

        IntegrationToken integrationToken = submission.getIntegrationToken();

        if (integrationToken == null) {
            // no token exists; create one for this submission
            integrationToken = IntegrationToken.builder()
                .integration(submission.getIntegration())
                .submission(submission)
                .token(buildToken())
                .user(submission.getParticipant().getLtiUserEntity())
                .build();
        }

        LocalDateTime launchedAt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

        if (integrationToken.getLastLaunchedAt() == null) {
            log.info(
                "Token [{}] launched at: [{}].",
                integrationToken.getToken(),
                launchedAt
            );
        } else {
            LocalDateTime previousLaunchedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(integrationToken.getLastLaunchedAt().getTime()), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

            log.info(
                "Token [{}] launched at: [{}]. Previous launch: [{}].",
                integrationToken.getToken(),
                launchedAt,
                previousLaunchedAt
            );
        }

        integrationToken.setSecuredInfo(securedInfo);
        integrationToken.setLastLaunchedAt(Timestamp.from(Instant.now()));

        try {
            // flushed immediately, rather than left for the surrounding @Transactional
            // method's own commit, so a concurrent launch of this same token - two requests
            // racing to update the same row - is caught here (where it can be handled)
            // instead of surfacing as an unhandled optimistic-locking failure once that
            // later commit happens
            integrationTokenRepository.saveAndFlush(integrationToken);
        } catch (Exception e) {
            // another concurrent launch of this same token already won the race and
            // persisted its own update - that update is just as valid as ours would have
            // been, so use the token as it now stands rather than writing over it again
            log.error("Error saving token for submission ID: [{}].", submission.getSubmissionId(), e);
            integrationToken = integrationTokenRepository.findBySubmission_SubmissionId(submission.getSubmissionId())
                .orElseThrow(() -> new IntegrationTokenNotFoundException(String.format("No token found for submission ID: [%s]", submission.getSubmissionId())));
        }

        submission.setIntegrationToken(integrationToken);
    }

    @Override
    public IntegrationToken findByToken(String token) throws IntegrationTokenNotFoundException {
        if (StringUtils.isBlank(token)) {
            throw new IntegrationTokenNotFoundException("Integration token cannot be blank");
        }

        return integrationTokenRepository.findByToken(token)
            .orElseThrow(() -> new IntegrationTokenNotFoundException(String.format("No integration token value [%s] found.", token)));
    }

    @Override
    public IntegrationToken redeemToken(String launchToken)
        throws DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenInvalidException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        if (StringUtils.isBlank(launchToken)) {
            throw new DataServiceException("No token passed in.");
        }

        IntegrationToken integrationToken = integrationTokenRepository.findByToken(launchToken)
            .orElseThrow(() -> new IntegrationTokenNotFoundException(String.format("No integration token found with launch token: [%s]", launchToken)));

        if (integrationToken.isAlreadyRedeemed()) {
            invalidate(integrationToken);
            throw new IntegrationTokenAlreadyRedeemedException(
                String.format(
                    "Integration token: [%s] was already redeemed at [%s].",
                    launchToken,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(integrationToken.getRedeemedAt().getTime()), ZoneOffset.UTC).toString()
                )
            );
        }

        if (integrationToken.isExpired(ttl)) {
            invalidate(integrationToken);
            throw new IntegrationTokenExpiredException(
                String.format(
                    "Integration token: [%s] expired at [%s].",
                    launchToken,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(integrationToken.getLastLaunchedAt().getTime() + (ttl * 1000L)), ZoneOffset.UTC).toString()
                )
            );
        }

        return invalidate(integrationToken);
    }

    private IntegrationToken invalidate(IntegrationToken integrationToken) {
        // set redeemed time to invalidate token
        integrationToken.setRedeemedAt(Timestamp.from(Instant.now()));

        return integrationTokenRepository.saveAndFlush(integrationToken);
    }

    private String buildToken() {
        return UUID.randomUUID().toString();
    }

}
