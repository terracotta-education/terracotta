package edu.iu.terracotta.model.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsentDto {

    private Long consentDocumentId;
    private String title;
    private String filePointer;
    private String html;
    private Integer expectedConsent;
    private Integer answeredConsentCount;

}
