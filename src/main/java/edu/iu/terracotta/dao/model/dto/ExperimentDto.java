package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentDto {

    private UUID experimentId;

    // Internal carriers only, threaded from SecuredInfo through fillContextInfo()/fromDto() to
    // build a new Experiment's LtiContextEntity/PlatformDeployment/createdBy relations on the
    // create-experiment flow - not populated by toDto() and never serialized to the frontend,
    // which has no legitimate use for another context/platform/LTI user's raw internal id.
    @JsonIgnore
    private Long platformDeploymentId;
    @JsonIgnore
    private Long contextId;
    @JsonIgnore
    private Long createdBy;

    private String title;
    private String description;
    private String exposureType;
    private String participationType;
    private String distributionType;
    private Timestamp started;
    private String createdByEmail;
    private Timestamp closed;
    private Integer potentialParticipants;
    private Integer acceptedParticipants;
    private Integer rejectedParticipants;
    private boolean exportEnabled;
    private List<FeatureDto> features;

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

}
