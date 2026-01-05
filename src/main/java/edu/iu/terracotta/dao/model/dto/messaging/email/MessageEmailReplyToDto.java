package edu.iu.terracotta.dao.model.dto.messaging.email;

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
public class MessageEmailReplyToDto {

    private UUID id;
    private UUID containerConfigurationId;
    private UUID messageConfigurationId;
    private String email;

}
