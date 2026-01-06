package edu.iu.terracotta.dao.entity.scheduledtask;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scheduled_tasks")
@JsonIgnoreProperties(ignoreUnknown = true)
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
