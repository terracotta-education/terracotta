package edu.iu.terracotta.service.app.integrations.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenAlreadyRedeemedException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenExpiredException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenInvalidException;
import edu.iu.terracotta.exceptions.integrations.IntegrationTokenNotFoundException;
import edu.iu.terracotta.model.app.Submission;
import edu.iu.terracotta.model.app.enumerator.QuestionTypes;
import edu.iu.terracotta.model.app.integrations.IntegrationToken;
import edu.iu.terracotta.model.oauth2.SecuredInfo;
import edu.iu.terracotta.repository.integrations.IntegrationTokenRepository;
import edu.iu.terracotta.service.app.integrations.IntegrationTokenService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class IntegrationTokenServiceImpl implements IntegrationTokenService {

    @Autowired private IntegrationTokenRepository integrationTokenRepository;

    @Value("${app.integrations.token.ttl:3600}")
    private int ttl;

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

        integrationToken.setSecuredInfo(securedInfo);
        integrationToken.setLastLaunchedAt(Timestamp.from(Instant.now()));

        try {
            integrationTokenRepository.save(integrationToken);
        } catch (Exception e) {
            log.error("Error saving token for submission ID: [{}].", submission.getSubmissionId(), e);
            // token exists; use that one
            integrationToken = integrationTokenRepository.findBySubmission_SubmissionId(submission.getSubmissionId())
                .orElseThrow(() -> new IntegrationTokenNotFoundException(String.format("No token found for submission ID: [%s]", submission.getSubmissionId())));
            integrationToken.setSecuredInfo(securedInfo);
            integrationToken.setLastLaunchedAt(Timestamp.from(Instant.now()));
            integrationTokenRepository.save(integrationToken);
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
            .orElseThrow(() -> new IntegrationTokenNotFoundException(String.format("No integration token found with token: [%s]", launchToken)));

        if (integrationToken.isAlreadyRedeemed()) {
            invalidate(integrationToken);
            throw new IntegrationTokenAlreadyRedeemedException(String.format("Integration token: [%s] has already been redeemed.", launchToken));
        }

        if (integrationToken.isExpired(ttl)) {
            invalidate(integrationToken);
            throw new IntegrationTokenExpiredException(String.format("Integration token: [%s] is expired.", launchToken));
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
