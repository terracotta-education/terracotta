package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// the POST body Canvas sends to a registered notice handler endpoint:
// {"notices": [{"jwt": "..."}, ...]}
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoticeRequestDto {

    private List<NoticeJwtDto> notices;

}
