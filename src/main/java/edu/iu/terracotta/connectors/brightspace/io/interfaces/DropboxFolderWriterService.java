package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolder;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolderUpdate;

public interface DropboxFolderWriterService extends BrightspaceWriterService<DropboxFolder, DropboxFolderWriterService> {

    Optional<DropboxFolder> create(String orgUnitId, DropboxFolderUpdate dropboxFolderUpdate) throws IOException;
    Optional<DropboxFolder> update(String orgUnitId, long dropboxFolderId, DropboxFolderUpdate dropboxFolderUpdate) throws IOException;
    void delete(String orgUnitId, long dropboxFolderId) throws IOException;

}
