package edu.iu.terracotta.service.app.impl;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.repository.ApiOneUseTokenRepository;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ScheduledServiceImpl {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired private ApiOneUseTokenRepository apiOneUseTokenRepository;

    @Scheduled(cron = "${scheduled.deleteoldtokens.cron:0 0 1 * * ?}")
    public void deleteOldTokens() {
        log.info("Deleting Old Tokens :: Starting - {} ", dateTimeFormatter.format(LocalDateTime.now()));
        apiOneUseTokenRepository.deleteByCreatedAtBefore(new Date(System.currentTimeMillis()-24*60*60*1000));
        log.info("Deleting Old Tokens :: Ended - {} ", dateTimeFormatter.format(LocalDateTime.now()));
    }

}