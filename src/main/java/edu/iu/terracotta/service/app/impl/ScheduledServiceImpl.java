package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.APIDataService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ScheduledService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ScheduledServiceImpl implements ScheduledService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private APIDataService apiDataService;

    @Scheduled(cron = "${scheduled.restoredeletedassignments.cron:0 0 3 * * ?}")
    public void restoreDeletedAssignments() throws DataServiceException, CanvasApiException, ConnectionException, IOException {
        log.info("Restoring Assignments :: Starting - {} ", dateTimeFormatter.format(LocalDateTime.now()));
        assignmentService.checkAndRestoreAllAssignmentsInCanvas();
        log.info("Restoring Assignments :: Ended - {} ", dateTimeFormatter.format(LocalDateTime.now()));
    }

    @Scheduled(cron = "${scheduled.deleteoldtokens.cron:0 0 1 * * ?}")
    public void deleteOldTokens(){
        log.info("Deleting Old Tokens :: Starting - {} ", dateTimeFormatter.format(LocalDateTime.now()));
        apiDataService.cleanOldTokens();
        log.info("Deleting Old Tokens :: Ended - {} ", dateTimeFormatter.format(LocalDateTime.now()));
    }

}