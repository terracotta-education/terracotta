package edu.iu.terracotta.runner.messaging;

import java.util.Optional;

import edu.iu.terracotta.runner.messaging.configuration.model.MessagingScheduleResult;

public interface MessagingSchedulerService {

    Optional<MessagingScheduleResult> send();

}
