package edu.iu.terracotta.runner.messaging.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MessagingScheduleResult {

    private List<MessagingScheduleMessage> processed;

}
