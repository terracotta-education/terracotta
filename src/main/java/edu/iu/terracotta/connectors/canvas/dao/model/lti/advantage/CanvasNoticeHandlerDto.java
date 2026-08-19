package edu.iu.terracotta.connectors.canvas.dao.model.lti.advantage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// body (and response) shape of Canvas's Notice Handlers API:
// PUT /api/lti/notice-handlers/:context_external_tool_id
// {"notice_type": "LtiContextCopyNotice", "handler": "https://our-tool.example.com/notice"}
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CanvasNoticeHandlerDto {

    private String notice_type;
    private String handler;

}
