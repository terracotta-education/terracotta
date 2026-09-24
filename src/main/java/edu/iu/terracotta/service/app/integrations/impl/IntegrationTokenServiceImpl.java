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
import org.springframework.transaction.annotation.Transactional;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationTokenServiceImpl implements IntegrationTokenService {

    private final IntegrationTokenRepository integrationTokenRepository;

    @PersistenceContext private EntityManager entityManager;

    @Value("${app.integrations.token.ttl:43200}")
    private long ttl;

    @Override
    @Transactional
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
        } else {
            // two launches of the same in-progress submission can race to stamp this one row.
            // Re-read it under a row lock (blocking behind any launch already mid-write) so
            // this update always targets the row's current version rather than the copy the
            // session loaded earlier. Without this, the loser's flush fails its version check
            // as an optimistic-locking error - and catching that inside the caller's
            // @Transactional is no recovery at all, since the repository proxy has already
            // marked the transaction rollback-only by then and the launch still fails at commit.
            entityManager.refresh(integrationToken, LockModeType.PESSIMISTIC_WRITE);
        }

        LocalDateTime launchedAt = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);

        if (integrationToken.getLastLaunchedAt() == null) {
            log.info(
                "Token [{}] launched at: [{}]",
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

        // flushed now rather than at commit so a genuine write failure surfaces here, at the
        // call that caused it, instead of as an opaque commit-time error
        integrationTokenRepository.saveAndFlush(integrationToken);

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
    @Transactional
    public IntegrationToken redeemToken(String launchToken)
        throws DataServiceException, IntegrationTokenNotFoundException, IntegrationTokenInvalidException, IntegrationTokenAlreadyRedeemedException, IntegrationTokenExpiredException {
        if (StringUtils.isBlank(launchToken)) {
            throw new DataServiceException("No token passed in.");
        }

        IntegrationToken integrationToken = integrationTokenRepository.findByToken(launchToken)
            .orElseThrow(() -> new IntegrationTokenNotFoundException(String.format("No integration token found with launch token: [%s]", launchToken)));

        // an external integration site's submit action isn't something Terracotta can debounce
        // (no control over that page's own button) - a double-click or a slow response that
        // gets retried sends two concurrent redemption attempts for the same token. Lock the row
        // before checking its redeemed state, so the second attempt blocks here until the
        // first's transaction commits, then correctly sees the row as already redeemed - without
        // this, both requests can read "not yet redeemed" before either writes, and both proceed
        // to score the same submission.
        entityManager.refresh(integrationToken, LockModeType.PESSIMISTIC_WRITE);

        if (integrationToken.isAlreadyRedeemed()) {
            // not re-invalidated here (unlike the expired branch below) - the token is already
            // redeemed, and calling invalidate() again would overwrite redeemedAt with this
            // duplicate attempt's own timestamp, corrupting the very value the message below
            // reports
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
