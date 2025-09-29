package edu.iu.terracotta.connectors.canvas.service.extended;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.FileExtended;
import edu.ksu.canvas.interfaces.CanvasReader;

public interface FileReaderExtended extends CanvasReader<FileExtended, FileReaderExtended> {

    Optional<FileExtended> getFile(String url) throws IOException;
    List<FileExtended> getFiles(String filesUrl) throws IOException;

}
