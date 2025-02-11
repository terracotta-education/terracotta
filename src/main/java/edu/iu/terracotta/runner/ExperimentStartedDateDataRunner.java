package edu.iu.terracotta.runner;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import edu.iu.terracotta.dao.entity.Assignment;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.repository.AssignmentRepository;
import edu.iu.terracotta.dao.repository.ExperimentRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentStartedDateDataRunner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private ExperimentRepository experimentRepository;

    @Value("${app.experiments.fix.start.dates.enabled:false}")
    private boolean enabled;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            return;
        }

        Thread thread = new Thread(
            () ->
                {
                    // fix experiment started date; if has an assignment started, mark experiment started, else set to null
                    List<Assignment> assignments = assignmentRepository.findAll();
                    List<Experiment> experiments = experimentRepository.findAll();

                    log.info("Starting experiment start date fix...");

                    CollectionUtils.emptyIfNull(experiments).stream()
                        .forEach(
                            experiment -> {
                                Optional<Assignment> startedAssignment = CollectionUtils.emptyIfNull(assignments).stream()
                                    .filter(assignment -> assignment.getExposure().getExperiment().getExperimentId().equals(experiment.getExperimentId()))
                                    .filter(assignment -> assignment.isStarted())
                                    .sorted(Comparator.comparing(Assignment::getStarted))
                                    .findFirst();

                                if (startedAssignment.isEmpty() && experiment.isStarted()) {
                                    // no started assignment and experiment is marked as started, set experiment started date to null
                                    experiment.setStarted(null);
                                    experimentRepository.save(experiment);
                                    return;
                                }

                                if (startedAssignment.isPresent() && !experiment.isStarted()) {
                                    // has an assignment started, but no start date for experiment; set the experiment start date to the assignment start date
                                    experiment.setStarted(startedAssignment.get().getStarted());
                                    experimentRepository.save(experiment);
                                }
                        });

                    log.info("Experiment start date fix complete! {} experiment records processed.", experiments.size());
                }
            );

            thread.start();
    }

}
