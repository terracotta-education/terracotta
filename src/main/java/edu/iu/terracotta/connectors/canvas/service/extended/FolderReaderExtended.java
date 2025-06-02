package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.FolderExtended;
import edu.ksu.canvas.interfaces.CanvasReader;

public interface FolderReaderExtended extends CanvasReader<FolderExtended, FolderReaderExtended> {

    Optional<FolderExtended> getFolder(String id) throws IOException;
    List<FolderExtended> getFolders() throws IOException;

}
