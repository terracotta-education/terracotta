
package edu.iu.terracotta.service.app.messaging;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainerConfiguration;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerConfigurationDto;

public interface MessageContainerConfigurationService {

    void create(MessageContainer container, LtiUserEntity owner);
    void update(MessageContainerConfigurationDto containerConfigurationDto, MessageContainerConfiguration containerConfiguration);
    void duplicate(MessageContainerConfiguration containerConfiguration, MessageContainer container);
    MessageContainerConfigurationDto toDto(MessageContainerConfiguration containerConfiguration);
    MessageContainerConfiguration fromDto(MessageContainerConfigurationDto containerConfigurationDto, MessageContainerConfiguration containerConfiguration);

}
