package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.service.app.APIDataService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ScheduledServiceImpl {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private APIDataService apiDataService;

    @Scheduled(cron = "${scheduled.deleteoldtokens.cron:0 0 1 * * ?}")
    public void deleteOldTokens(){
        log.info("Deleting Old Tokens :: Starting - {} ", dateTimeFormatter.format(LocalDateTime.now()));
        apiDataService.cleanOldTokens();
        log.info("Deleting Old Tokens :: Ended - {} ", dateTimeFormatter.format(LocalDateTime.now()));
    }

}