package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ExportService {
    Map<String, List<String[]>> getCsvFiles(Long experimentId, SecuredInfo securedInfo) throws CanvasApiException, ParticipantNotUpdatedException, IOException;
    Map<String, String> getJsonFiles(Long experimentId);
    Map<String, String> getReadMeFile() throws IOException;

}

