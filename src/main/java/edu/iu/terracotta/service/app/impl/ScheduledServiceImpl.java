package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.exceptions.AssignmentNotCreatedException;
import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ConnectionException;
import edu.iu.terracotta.exceptions.DataServiceException;
import edu.iu.terracotta.service.app.APIDataService;
import edu.iu.terracotta.service.app.AssignmentService;
import edu.iu.terracotta.service.app.ScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ScheduledServiceImpl implements ScheduledService {

    static final Logger log = LoggerFactory.getLogger(ScheduledServiceImpl.class);
    private static final DateTimeFormatter dateTimeFormatter= DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    AssignmentService assignmentService;

    @Autowired
    APIDataService apiDataService;


    @Scheduled(cron = "${scheduled.restoredeletedassignments.cron:0 0 3 * * ?}")
    public void restoreDeletedAssignments() throws DataServiceException, CanvasApiException, ConnectionException, IOException, AssignmentNotCreatedException {
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