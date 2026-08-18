package edu.iu.terracotta.runner.lmsuserbatchcleaner.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LmsUserBatchCleanerScheduleResult {

    private List<LmsUserBatchCleanerScheduleMessage> processed;

}
