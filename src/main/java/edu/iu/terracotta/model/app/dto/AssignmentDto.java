package edu.iu.terracotta.model.app.dto;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.model.app.enumerator.MultipleSubmissionScoringScheme;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentDto {

    private Long assignmentId;
    private Long exposureId;
    private String title;
    private String lmsAssignmentId;
    private Integer assignmentOrder;
    private String resourceLinkId;
    private boolean started;
    private Boolean softDeleted;
    private Integer numOfSubmissions;
    private Float hoursBetweenSubmissions;
    private String multipleSubmissionScoringScheme = MultipleSubmissionScoringScheme.MOST_RECENT.name();
    private Float cumulativeScoringInitialPercentage;
    private List<TreatmentDto> treatments;
    private boolean allowStudentViewResponses = false;
    private Timestamp studentViewResponsesAfter;
    private Timestamp studentViewResponsesBefore;
    private boolean allowStudentViewCorrectAnswers = false;
    private Timestamp studentViewCorrectAnswersAfter;
    private Timestamp studentViewCorrectAnswersBefore;
    private boolean published;
    private Date dueDate;

}
