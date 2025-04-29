package edu.iu.terracotta.service.app.export.data.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import edu.iu.terracotta.connectors.generic.dao.model.SecuredInfo;
import edu.iu.terracotta.connectors.generic.dao.repository.lti.LtiUserRepository;
import edu.iu.terracotta.connectors.generic.exceptions.ApiException;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.dao.entity.Experiment;
import edu.iu.terracotta.dao.entity.Submission;
import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.exceptions.ExperimentNotMatchingException;
import edu.iu.terracotta.dao.exceptions.OutcomeNotMatchingException;
import edu.iu.terracotta.dao.exceptions.ParticipantNotUpdatedException;
import edu.iu.terracotta.dao.model.dto.export.data.ExperimentDataExportDto;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.dao.repository.SubmissionRepository;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportNotFoundException;
import edu.iu.terracotta.exceptions.export.data.ExperimentDataExportException;
import edu.iu.terracotta.service.app.FileStorageService;
import edu.iu.terracotta.service.app.async.ExperimentDataExportAsyncService;
import edu.iu.terracotta.service.app.export.data.ExperimentDataExportService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentDataExportServiceImpl implements ExperimentDataExportService {

    @Autowired private ExperimentDataExportRepository experimentDataExportRepository;
    @Autowired private LtiUserRepository ltiUserRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private ExperimentDataExportAsyncService experimentDataExportAsyncService;
    @Autowired private FileStorageService fileStorageService;

    @Override
    public ExperimentDataExportDto process(Experiment experiment, SecuredInfo securedInfo)
        throws IOException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException,
        ApiException, TerracottaConnectorException {
        return process(experiment, securedInfo, ExperimentDataExportStatus.PROCESSING);
    }

    private ExperimentDataExportDto process(Experiment experiment, SecuredInfo securedInfo, ExperimentDataExportStatus exportDataStatus)
        throws IOException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException,
        ApiException, TerracottaConnectorException {
        LtiUserEntity owner = ltiUserRepository.findByUserKeyAndPlatformDeployment_KeyId(securedInfo.getUserId(), securedInfo.getPlatformDeploymentId());
        log.info("User with ID: [{}] is processing data export for experiment with ID: [{}].", owner.getUserId(), experiment.getExperimentId());
        ExperimentDataExport exportData = ExperimentDataExport.builder()
            .experiment(experiment)
            .owner(owner)
            .status(exportDataStatus)
            .build();

        exportData = experimentDataExportRepository.save(exportData);

        experimentDataExportAsyncService.process(exportData, securedInfo);

        log.info("Experiment data export with ID: [{}] is being processed.", exportData.getUuid());
        return toDto(exportData, false);
    }

    @Override
    public ExperimentDataExportDto poll(Experiment experiment, SecuredInfo securedInfo, boolean createNewOnOutdated)
        throws IOException, ExperimentDataExportNotFoundException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException,
        OutcomeNotMatchingException, ApiException, TerracottaConnectorException {
        ExperimentDataExport experimentDataExport = experimentDataExportRepository.findTopByExperiment_ExperimentIdOrderByCreatedAtDesc(experiment.getExperimentId())
            .orElseThrow(() -> new ExperimentDataExportNotFoundException(String.format("No experiment data export with experiment ID: [%s] found.", experiment.getExperimentId())));

        // if exported data is marked as outdated and acknowledged, don't process it
        if (ExperimentDataExportStatus.OUTDATED_ACKNOWLEDGED == experimentDataExport.getStatus()) {
            throw new ExperimentDataExportNotFoundException(String.format("No active experiment data export with experiment ID: [%s] found.", experiment.getExperimentId()));
        }

        // if exported data is current, return it; else process a new one
        if (isExperimentDataExportCurrent(experimentDataExport)) {
            return toDto(experimentDataExport, false);
        }

        experimentDataExport.setStatus(ExperimentDataExportStatus.OUTDATED);
        experimentDataExportRepository.save(experimentDataExport);

        if (!createNewOnOutdated) {
            return toDto(experimentDataExport, false);
        }

        return process(experiment, securedInfo, ExperimentDataExportStatus.REPROCESSING);
    }

    @Override
    public List<ExperimentDataExportDto> poll(List<Experiment> experiments, SecuredInfo securedInfo, boolean createNewOnOutdated) throws IOException, ExperimentDataExportNotFoundException,
        NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException, OutcomeNotMatchingException, ApiException, TerracottaConnectorException {
        return experiments.stream()
            .map(experiment -> {
                try {
                    return poll(experiment, securedInfo, createNewOnOutdated);
                } catch (ExperimentDataExportNotFoundException e) {
                    // no experiment data export found for this experiment; swallow exception, as this is common
                    return null;
                } catch (IOException | NumberFormatException | ExperimentDataExportException | ParticipantNotUpdatedException | ExperimentNotMatchingException | OutcomeNotMatchingException | ApiException | TerracottaConnectorException e) {
                    log.error("Error polling experiment data export for experiment ID: [{}].", experiment.getExperimentId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public ExperimentDataExportDto retrieve(UUID uuid, Experiment experiment, SecuredInfo securedInfo)
        throws IOException, NumberFormatException, ExperimentDataExportException, ParticipantNotUpdatedException, ExperimentNotMatchingException,
        OutcomeNotMatchingException, ApiException, TerracottaConnectorException {
        Optional<ExperimentDataExport> experimentDataExport = findLatestAvailableExperimentDataExport(experiment.getExperimentId());

        if (experimentDataExport.isPresent()) {
            // existing valid data export found; return it
            experimentDataExport.get().setStatus(ExperimentDataExportStatus.DOWNLOADED);
            experimentDataExportRepository.save(experimentDataExport.get());

            return toDto(experimentDataExport.get(), true);
        }

        // no existing valid data export found; process a new one
        return process(experiment, securedInfo);
    }

    @Override
    public Optional<ExperimentDataExport> findLatestAvailableExperimentDataExport(long experimentId) throws IOException {
        Optional<ExperimentDataExport> experimentDataExport = experimentDataExportRepository.findTopByExperiment_ExperimentIdAndStatusInOrderByCreatedAtDesc(
            experimentId,
            Arrays.asList(
                ExperimentDataExportStatus.DOWNLOADED,
                ExperimentDataExportStatus.READY,
                ExperimentDataExportStatus.READY_ACKNOWLEDGED
            )
        );

        if (experimentDataExport.isEmpty()) {
            // No available export data found for the experiment
            return Optional.empty();
        }

        return isExperimentDataExportCurrent(experimentDataExport.get()) ? experimentDataExport : Optional.empty();
    }

    @Override
    public ExperimentDataExportDto acknowledge(UUID uuid, Experiment experiment, ExperimentDataExportStatus experimentDataExportStatus) throws IOException, ExperimentDataExportNotFoundException {
        ExperimentDataExport experimentDataExport = experimentDataExportRepository.findByUuidAndExperiment_ExperimentId(uuid, experiment.getExperimentId())
            .orElseThrow(() -> new ExperimentDataExportNotFoundException(String.format("No data export with experiment ID: [%s] found.", experiment.getExperimentId())));

        experimentDataExport.setStatus(experimentDataExportStatus);

        return toDto(experimentDataExportRepository.save(experimentDataExport), false);
    }

    @Override
    public ExperimentDataExportDto toDto(ExperimentDataExport experimentDataExport, boolean includeFileContent) throws IOException {
        return ExperimentDataExportDto.builder()
        .experimentId(experimentDataExport.getExperimentId())
            .experimentTitle(experimentDataExport.getExperimentTitle())
            .id(experimentDataExport.getUuid())
            .file(includeFileContent ? fileStorageService.getExperimentDataExport(experimentDataExport.getId()) : null)
            .fileName(
                StringUtils.isNotBlank(experimentDataExport.getFileName()) ?
                    experimentDataExport.getFileName()
                    : null
            )
            .mimeType(experimentDataExport.getMimeType())
            .status(experimentDataExport.getStatus())
            .build();
    }

    private boolean isExperimentDataExportCurrent(ExperimentDataExport experimentDataExport) {
        // get latest submission for the assignment
        Optional<Submission> submission = submissionRepository.findTopByParticipant_Experiment_ExperimentIdAndDateSubmittedNotNullOrderByDateSubmittedDesc(experimentDataExport.getExperimentId());

        // no submissions or export data is older than the latest submission
        return submission.isEmpty() || experimentDataExport.getCreatedAt().after(submission.get().getDateSubmitted());
    }

}
