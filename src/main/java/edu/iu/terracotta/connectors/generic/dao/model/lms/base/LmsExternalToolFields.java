package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LmsExternalToolFields {

    private String url;
    private String resourceLinkId;

}
