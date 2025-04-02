package edu.iu.terracotta.runner.export.data.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.export.data.ExperimentDataExport;
import edu.iu.terracotta.dao.model.enums.export.data.ExperimentDataExportStatus;
import edu.iu.terracotta.dao.repository.export.data.ExperimentDataExportRepository;
import edu.iu.terracotta.runner.export.data.ExperimentDataExportSchedulerService;
import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleMessage;
import edu.iu.terracotta.runner.export.data.model.ExperimentDataExportScheduleResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentDataExportSchedulerServiceImpl implements ExperimentDataExportSchedulerService {

    @Autowired private ExperimentDataExportRepository experimentDataExportRepository;

    @Value("${experiment.data.export.local.path.root}")
    private String experimentDataExportLocalPathRoot;

    @Value("${experiment.data.export.ttl.days:7}")
    private int ttl;

    @Override
    public Optional<ExperimentDataExportScheduleResult> cleanup() {
        List<ExperimentDataExport> expiredExperimentDataExports = experimentDataExportRepository.findAllByUpdatedAtLessThanAndStatusIn(
            Timestamp.from(Instant.now().minus(Duration.ofDays(ttl))),
            List.of(
                ExperimentDataExportStatus.DOWNLOADED,
                ExperimentDataExportStatus.ERROR,
                ExperimentDataExportStatus.ERROR_ACKNOWLEDGED,
                ExperimentDataExportStatus.OUTDATED,
                ExperimentDataExportStatus.READY,
                ExperimentDataExportStatus.READY_ACKNOWLEDGED
            )
        );

        if (CollectionUtils.isEmpty(expiredExperimentDataExports)) {
            // no expired experiment data exports exist; exit
            return Optional.empty();
        }

        Optional<ExperimentDataExportScheduleResult> results = Optional.of(
            ExperimentDataExportScheduleResult.builder()
                .processed(
                    expiredExperimentDataExports.stream()
                        .map(
                            expiredExperimentDataExport -> {
                                String error = null;
                                List<Boolean> deleted = new ArrayList<>();
                                try {
                                    deleted.add(
                                        Paths.get(
                                            String.format(
                                                "%s/%s",
                                                experimentDataExportLocalPathRoot,
                                                expiredExperimentDataExport.getFileUri()
                                            )
                                        )
                                        .toFile()
                                        .delete()
                                    );
                                    deleted.add(
                                        Paths.get(
                                            String.format(
                                                "%s/%s%s",
                                                experimentDataExportLocalPathRoot,
                                                expiredExperimentDataExport.getFileUri(),
                                                ExperimentDataExport.COMPRESSED_FILE_EXTENSION
                                            )
                                        )
                                        .toFile()
                                        .delete()
                                    );
                                } catch (SecurityException e) {
                                    error = e.getMessage();
                                }

                                if (deleted.stream().anyMatch(deletedFile -> !deletedFile)) {
                                    error = String.format("Failed to delete file URI: [%s]", expiredExperimentDataExport.getFileUri());
                                }

                                expiredExperimentDataExport.setStatus(ExperimentDataExportStatus.DELETED);
                                experimentDataExportRepository.save(expiredExperimentDataExport);

                                return ExperimentDataExportScheduleMessage.builder()
                                    .deletedAt(Timestamp.from(Instant.now()))
                                    .errors(StringUtils.isEmpty(error) ? null : List.of(error))
                                    .fileName(expiredExperimentDataExport.getFileName())
                                    .fileUri(expiredExperimentDataExport.getFileUri())
                                    .id(expiredExperimentDataExport.getId())
                                    .build();
                            }
                        )
                        .toList()
                )
                .build()
        );

        // delete any empty directories
        FileUtils.listFilesAndDirs(
            Paths.get(experimentDataExportLocalPathRoot).toFile(),
            FileFilterUtils.trueFileFilter(),
            FileFilterUtils.trueFileFilter()
        )
        .stream()
        .filter(File::isDirectory)
        .filter(
            directory -> {
                try {
                    return FileUtils.isEmptyDirectory(directory);
                } catch (IOException e) {
                    log.warn("Error checking directory is empty: [{}]", directory.getAbsolutePath(), e);
                    return false;
                }
            }
        )
        .forEach(
            directory -> {
                try {
                    FileUtils.deleteDirectory(directory);
                    log.info("Experiment data export cleanup deleted empty directory: [{}]", directory.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("Error deleting directory: [{}]", directory.getAbsolutePath(), e);
                }
            }
        );

        return results;
    }

}
