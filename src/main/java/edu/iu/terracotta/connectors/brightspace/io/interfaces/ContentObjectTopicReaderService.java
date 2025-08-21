package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopic;

public interface ContentObjectTopicReaderService extends BrightspaceReaderService<ContentObjectTopic, ContentObjectTopicReaderService> {

    Optional<ContentObjectTopic> get(String orgUnitId, long contentObjectTopicId) throws IOException;

}
