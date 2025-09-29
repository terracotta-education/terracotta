package edu.iu.terracotta.service.app.messaging;

import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.dao.entity.Exposure;
import edu.iu.terracotta.dao.entity.messaging.container.MessageContainer;
import edu.iu.terracotta.dao.model.dto.messaging.container.MessageContainerDto;
import edu.iu.terracotta.exceptions.messaging.MessageBodyParseException;

public interface MessageContainerService {

    List<MessageContainerDto> getAll(long experimentId, long exposureId, SecuredInfo securedInfo);
    MessageContainerDto create(Exposure exposure, boolean single, SecuredInfo securedInfo);
    MessageContainerDto update(MessageContainerDto containerDto, MessageContainer container);
    List<MessageContainerDto> updateAll(List<MessageContainerDto> containerDtos, List<MessageContainer> containers);
    MessageContainerDto delete(MessageContainer container);
    MessageContainerDto move(Exposure exposure, MessageContainer container);
    MessageContainerDto duplicate(MessageContainer container, Exposure exposure) throws MessageBodyParseException;
    List<MessageContainerDto> toDto(List<MessageContainer> containers);
    MessageContainerDto toDto(MessageContainer container);

}
