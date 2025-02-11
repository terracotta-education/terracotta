package edu.iu.terracotta.connectors.generic.dao.model.lti.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LineItems {

    @Builder.Default private List<LineItem> lineItemList = new ArrayList<>();

}
