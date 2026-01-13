package edu.iu.terracotta.connectors.generic.dao.model.lti.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class LineItem {

    @JsonProperty private String id;
    @JsonProperty private Float scoreMaximum;
    @JsonProperty private String label;
    @JsonProperty private String resourceId;
    @JsonProperty private String tag;
    @JsonProperty private String resourceLinkId;
    @JsonProperty private String startDateTime;
    @JsonProperty private String endDateTime;

}
