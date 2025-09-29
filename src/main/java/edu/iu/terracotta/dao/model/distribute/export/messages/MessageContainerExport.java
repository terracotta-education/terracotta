package edu.iu.terracotta.dao.model.distribute.export.messages;

import java.util.List;

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
public class MessageContainerExport {

    private long id;
    private long exposureId;
    private List<MessageExport> messages;
    private MessageContainerConfigurationExport configuration;

}
