package edu.iu.terracotta.service.app;

import edu.iu.terracotta.exceptions.CanvasApiException;
import edu.iu.terracotta.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.model.oauth2.SecuredInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ExportService {

    Map<String, List<String[]>> getCsvFiles(long experimentId, SecuredInfo securedInfo) throws CanvasApiException, ParticipantNotUpdatedException, IOException, ExperimentNotMatchingException, OutcomeNotMatchingException;

    Map<String, String> getJsonFiles(Long experimentId);

    Map<String, String> getReadMeFile() throws IOException;

}
