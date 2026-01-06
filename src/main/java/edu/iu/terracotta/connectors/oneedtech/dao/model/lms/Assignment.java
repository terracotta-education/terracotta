package edu.iu.terracotta.connectors.oneedtech.dao.model.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
public class Assignment {

    private String id;
    private String resourceId;
    private String resourceLinkId;
    private String label;
    private String tag;
    private Float scoreMaximum;
    private String startDateTime;
    private String endDateTime;

}
