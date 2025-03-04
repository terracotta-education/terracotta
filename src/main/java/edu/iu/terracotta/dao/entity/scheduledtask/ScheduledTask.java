package edu.iu.terracotta.dao.entity.scheduledtask;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "scheduled_tasks")
public class ScheduledTask {

    @EmbeddedId private ScheduledTaskId id;

    @Column(
        insertable = false,
        updatable = false
    )
    private String taskName;

    @Column private boolean picked;
    @Column private String pickedBy;

}
