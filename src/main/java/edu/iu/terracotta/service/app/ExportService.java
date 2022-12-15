package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import java.io.IOException;
import java.util.Map;

public interface ExportService {

    Map<String, String> getFiles(long experimentId, SecuredInfo securedInfo) throws CanvasApiException, ParticipantNotUpdatedException, IOException;

}
