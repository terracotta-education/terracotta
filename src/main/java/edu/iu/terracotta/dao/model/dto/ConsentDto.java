package edu.iu.terracotta.dao.model.dto;

import java.util.UUID;

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
public class ConsentDto {

    private UUID consentDocumentId;
    private String title;
    private String filePointer;
    private String html;
    private Integer expectedConsent;
    private Integer answeredConsentCount;

}
