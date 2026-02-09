package edu.iu.terracotta.connectors.brightspace.io.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Submission extends BaseBrightspaceModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String assignmentId;
    private Assignment assignment;
    private Course course;
    private Long attempt;
    private String body;
    private String grade;
    private Boolean gradeMatchesCurrentSubmission;
    private String htmlUrl;
    private String previewUrl;
    private Double score;
    private String submissionType;
    private Date submittedAt;
    private String url;
    private String userId;
    private String gradeId;
    private User user;
    private Boolean late;
    private Boolean assigmentVisible;
    private Boolean excused;
    private String workflowState;
    private List<Submission> submissionHistory;

}
