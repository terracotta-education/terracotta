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

@Table(name = "terr_file_info")
@Entity
public class FileInfo extends BaseEntity {
    @Column(name = "file_id", nullable = false)
    @Id
    private String fileId;

    @Column(name = "filename")
    private String filename;

    @JoinColumn(name = "experiment_experiment_id", nullable = false)
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Experiment experiment;

    @Column(name = "size")
    private Long size;

    @Column(name = "file_type")
    private String fileType;


    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilename() { return filename; }

    public void setFilename(String filename) { this.filename = filename; }

    public Experiment getExperiment() { return experiment; }

    public void setExperiment(Experiment experiment) { this.experiment = experiment; }

    public Long getSize() { return size; }

    public void setSize(Long size) { this.size = size; }

    public String getFileType() { return fileType; }

    public void setFileType(String fileType) { this.fileType = fileType; }
}