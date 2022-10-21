package edu.iu.terracotta.model.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantDto {

    private Long participantId;
    private Long experimentId;
    private UserDto user;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp createdAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Timestamp updatedAt;

    private Boolean consent;
    private Timestamp dateGiven;
    private Timestamp dateRevoked;
    private String source;
    private Boolean dropped;
    private Long groupId;
    private boolean started;

}
