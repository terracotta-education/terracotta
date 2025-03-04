package edu.iu.terracotta.runner.assignmentfilearchive.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AssignmentFileArchiveScheduleResult {

    private List<AssignmentFileArchiveScheduleMessage> processed;

}
