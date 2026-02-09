package edu.iu.terracotta.dao.entity.scheduledtask;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;

@Embeddable
@EqualsAndHashCode
public class ScheduledTaskId implements Serializable {

    public String taskName;
    public String taskInstance;

}
