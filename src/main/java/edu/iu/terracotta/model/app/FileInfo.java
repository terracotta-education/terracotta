package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_file_info")
public class FileInfo extends BaseEntity {

    @Id
    @Column(nullable = false)
    private String fileId;

    @Column
    private String filename;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    private Experiment experiment;

    @Column
    private Long size;

    @Column
    private String fileType;

}
