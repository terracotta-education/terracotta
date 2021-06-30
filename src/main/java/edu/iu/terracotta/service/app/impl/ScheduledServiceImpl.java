package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.service.app.ScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ScheduledServiceImpl implements ScheduledService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledServiceImpl.class);
    private static final DateTimeFormatter dateTimeFormatter= DateTimeFormatter.ofPattern("HH:mm:ss");

    @Scheduled(cron = "${scheduled.hello.cron:0 0 0 * * ?}")
    public void hello(){
        log.info("Hello :: Execution Time - {} ", dateTimeFormatter.format(LocalDateTime.now()));
    }
}
