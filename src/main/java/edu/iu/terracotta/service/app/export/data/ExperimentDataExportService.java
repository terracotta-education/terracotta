package edu.iu.terracotta.service.app.export.data;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.export.data.ExperimentDataExportDto;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportNotFoundException;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportException;

public interface ExperimentDataExportService {

    ExperimentDataExportDto process(Experiment experiment, SecuredInfo securedInfo) throws IOException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, TerracottaConnectorException;
    ExperimentDataExportDto poll(Experiment experiment, SecuredInfo securedInfo, boolean createNewOnOutdated) throws IOException, ExperimentDataExportNotFoundException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, TerracottaConnectorException;
    List<ExperimentDataExportDto> poll(List<Experiment> experiments, SecuredInfo securedInfo, boolean createNewOnOutdated) throws IOException, ExperimentDataExportNotFoundException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, TerracottaConnectorException;
    ExperimentDataExportDto retrieve(UUID uuid, Experiment experiment, SecuredInfo securedInfo) throws IOException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, TerracottaConnectorException;
    Optional<ExperimentDataExport> findLatestAvailableExperimentDataExport(long experimentId) throws IOException;
    ExperimentDataExportDto acknowledge(UUID uuid, Experiment experiment, ExperimentDataExportStatus experimentDataExportStatus) throws IOException, ExperimentDataExportNotFoundException;
    ExperimentDataExportDto toDto(ExperimentDataExport exportData, boolean includeFileContent) throws IOException;

}
