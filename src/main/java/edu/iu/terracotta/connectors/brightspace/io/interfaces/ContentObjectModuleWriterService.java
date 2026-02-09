package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModule;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModuleUpdate;

public interface ContentObjectModuleWriterService extends BrightspaceWriterService<ContentObjectModule, ContentObjectModuleWriterService> {

    Optional<ContentObjectModule> create(String orgUnitId, ContentObjectModuleUpdate contentObjectModuleUpdate) throws IOException;
    Optional<ContentObjectModule> update(String orgUnitId, long contentObjectModuleId, ContentObjectModuleUpdate contentObjectModuleUpdate) throws IOException;
    void delete(String orgUnitId, long contentObjectModuleId) throws IOException;

}
