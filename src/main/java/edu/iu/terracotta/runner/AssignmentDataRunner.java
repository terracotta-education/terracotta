package edu.iu.terracotta.runner;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import edu.iu.terracotta.model.app.Assignment;
import edu.iu.terracotta.repository.AssignmentRepository;
import edu.iu.terracotta.repository.SubmissionRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AssignmentDataRunner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;

    @Value("${app.assignments.fix.start.dates.enabled:false}")
    private boolean enabled;

    @Value("${app.assignments.fix.start.dates.batchsize:100}")
    private int batchSize;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            return;
        }

        Thread thread = new Thread(
            () ->
                {
                    // fix legacy assignment started date
                    int page = 0;
                    Page<Assignment> assignments = assignmentRepository.findAll(PageRequest.of(page++, batchSize));

                    log.info("Starting assignment start date fix...");
                    int processed = 0;

                    while (CollectionUtils.isNotEmpty(assignments.getContent())) {
                        processed += assignments.getContent().size();
                        assignments.getContent().stream()
                            .filter(assignment -> !assignment.isStarted())
                            .forEach(
                                assignment -> {
                                    long submissionsCount = submissionRepository.countByAssessment_Treatment_Assignment_AssignmentId(assignment.getAssignmentId());

                                    if (submissionsCount > 0) {
                                        assignment.setStarted(Timestamp.valueOf(LocalDateTime.now()));
                                        assignmentRepository.save(assignment);
                                    }
                                }
                            );

                        log.info("Processed {} assignment records...", processed);
                        assignments = assignmentRepository.findAll(PageRequest.of(page++, batchSize));
                    }

                    log.info("Assignment start date fix complete! {} assignment records processed.", processed);
                }
        );

        thread.start();
    }

}
