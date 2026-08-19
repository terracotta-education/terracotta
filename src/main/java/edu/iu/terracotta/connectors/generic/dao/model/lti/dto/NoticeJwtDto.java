package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// one element of the "notices" array Canvas POSTs to a registered notice handler endpoint:
// {"notices": [{"jwt": "..."}, ...]}
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoticeJwtDto {

    private String jwt;

}
