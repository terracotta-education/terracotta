package edu.iu.terracotta.connectors.oneedtech.dao.model.lms;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
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
