package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
