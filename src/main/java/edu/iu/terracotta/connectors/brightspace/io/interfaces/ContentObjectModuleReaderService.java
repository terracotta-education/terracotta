package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModule;

public interface ContentObjectModuleReaderService extends BrightspaceReaderService<ContentObjectModule, ContentObjectModuleReaderService> {

    List<ContentObjectModule> getAllForOrgUnitId(String orgUnitId) throws IOException;
    Optional<ContentObjectModule> get(String orgUnitId, long contentObjectModuleId) throws IOException;
}
