package edu.iu.terracotta.runner.apitokencleaner.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiTokenCleanerScheduleResult {

    private List<ApiTokenCleanerScheduleMessage> processed;

}
