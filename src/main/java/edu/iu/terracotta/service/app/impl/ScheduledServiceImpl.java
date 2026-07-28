package edu.iu.terracotta.service.app.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOneUseTokenRepository;
import edu.iu.terracotta.dao.repository.LtiNonceRepository;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ScheduledServiceImpl {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ApiOneUseTokenRepository apiOneUseTokenRepository;
    private final LtiNonceRepository ltiNonceRepository;

    @Scheduled(cron = "${scheduled.deleteoldtokens.cron:0 0 1 * * ?}")
    public void deleteOldTokens() {
        log.info("Deleting Old Tokens :: Starting - {} ", dateTimeFormatter.format(LocalDateTime.now()));
        apiOneUseTokenRepository.deleteByCreatedAtBefore(new Date(System.currentTimeMillis()-24*60*60*1000));
        log.info("Deleting Old Tokens :: Ended - {} ", dateTimeFormatter.format(LocalDateTime.now()));
    }

    // OIDC login-initiation nonces are consumed (deleted) as soon as a launch completes; anything still
    // here past an hour is from an abandoned launch and just needs periodic cleanup.
    @Scheduled(cron = "${scheduled.deleteoldnonces.cron:0 0 * * * ?}")
    public void deleteOldNonces() {
        long deleted = ltiNonceRepository.deleteByCreatedAtBefore(new Date(System.currentTimeMillis() - 60 * 60 * 1000));

        if (deleted > 0) {
            log.info("Deleted {} old nonce(s) - {} ", deleted, dateTimeFormatter.format(LocalDateTime.now()));
        }
    }

}