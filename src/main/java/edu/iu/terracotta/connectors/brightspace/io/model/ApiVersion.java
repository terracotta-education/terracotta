package edu.iu.terracotta.connectors.brightspace.io.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiVersion {

    private String lp;
    private String le;

}
