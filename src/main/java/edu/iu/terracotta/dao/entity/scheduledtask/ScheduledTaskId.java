package edu.iu.terracotta.dao.entity.scheduledtask;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

@Embeddable
public class ScheduledTaskId implements Serializable {

    public String taskName;
    public String taskInstance;

}
