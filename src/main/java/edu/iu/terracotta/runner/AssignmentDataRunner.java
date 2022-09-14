package edu.iu.terracotta.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.repository.AllRepositories;


@Component
public class AssignmentDataRunner implements ApplicationListener<ApplicationReadyEvent> {

    static final Logger log = LoggerFactory.getLogger(AssignmentDataRunner.class);

    @Autowired
    private AllRepositories allRepositories;

    @Value("${app.assignments.fix.start.dates:false}")
    private boolean enabled;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            return;
        }

        // fix legacy assignment started date
        List<Assignment> assignments = allRepositories.assignmentRepository.findAll();

        log.info("Starting assignment start date fix...");

        assignments.stream()
            .filter(assignment -> { return !assignment.isStarted(); })
            .forEach(
                assignment -> {
                    long submissionsCount = allRepositories.submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());
    
                    if (submissionsCount > 0) {
                        assignment.setStarted(Timestamp.valueOf(LocalDateTime.now()));
                        allRepositories.assignmentRepository.save(assignment);
                    }
                }
            );

        log.info("Assignment start date fix complete!");
    }

}
