package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolder;

public interface DropboxFolderReaderService extends BrightspaceReaderService<DropboxFolder, DropboxFolderReaderService> {

    List<DropboxFolder> getAllForOrgUnitId(String orgUnitId) throws IOException;
    Optional<DropboxFolder> get(String orgUnitId, long dropboxFolderId) throws IOException;

}
