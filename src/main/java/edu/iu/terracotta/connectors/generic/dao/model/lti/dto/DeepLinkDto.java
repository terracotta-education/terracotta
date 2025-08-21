package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepLinkDto {

    private String toolLinkId;
    private String title;
    private String description;

    public DeepLinkDto(String toolLinkId, String title, String description) {
        this.toolLinkId = toolLinkId;
        this.title = title;
        this.description = description;
    }

    public DeepLinkDto(HttpServletRequest req) {
        this(
            req.getParameter("toolLinkId"),
            req.getParameter("title"),
            req.getParameter("description")
        );
    }

}
