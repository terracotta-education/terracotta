package edu.iu.terracotta.dao.model.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsUserBatchStatusDto {

    private UUID batchId;
    private LmsUserBatchStatus status;
    private String message;

}
