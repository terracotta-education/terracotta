package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopic;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopicUpdate;

public interface ContentObjectTopicWriterService extends BrightspaceWriterService<ContentObjectTopic, ContentObjectTopicWriterService> {

    Optional<ContentObjectTopic> create(String orgUnitId, long contentObjectModuleId, ContentObjectTopicUpdate contentObjectTopicUpdate) throws IOException;
    Optional<ContentObjectTopic> update(String orgUnitId, long contentObjectTopicId, ContentObjectTopicUpdate contentObjectTopicUpdate) throws IOException;
    void delete(String orgUnitId, long contentObjectTopicId) throws IOException;

}
