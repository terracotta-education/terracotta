package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentDto {

    private Long experimentId;
    private Long platformDeploymentId;
    private Long contextId;
    private String title;
    private String description;
    private String exposureType;
    private String participationType;
    private String distributionType;
    private Timestamp started;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp createdAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp updatedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ConditionDto> conditions;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ExposureDto> exposures;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ParticipantDto> participants;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ConsentDto consent;

    private Long createdBy;
    private Timestamp closed;
    private Integer potentialParticipants;
    private Integer acceptedParticipants;
    private Integer rejectedParticipants;

}
