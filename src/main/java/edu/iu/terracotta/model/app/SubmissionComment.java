package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "terr_submission_comment")
public class SubmissionComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_comment_id", nullable = false)
    private Long submissionCommentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submission_submission_id", nullable = false)
    private Submission submission;

    @Lob
    @Column
    private String comment;

    @Column
    private String creator;

}
