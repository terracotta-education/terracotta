package edu.iu.terracotta.runner.distribute.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.dao.entity.distribute.ExperimentImport;
import edu.iu.terracotta.dao.model.enums.distribute.ExperimentImportStatus;
import edu.iu.terracotta.dao.repository.distribute.ExperimentImportRepository;
import edu.iu.terracotta.runner.distribute.ExperimentImportSchedulerService;
import edu.iu.terracotta.runner.distribute.model.ExperimentImportScheduleMessage;
import edu.iu.terracotta.runner.distribute.model.ExperimentImportScheduleResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ExperimentImportSchedulerServiceImpl implements ExperimentImportSchedulerService {

    @Autowired private ExperimentImportRepository experimentImportRepository;

    @Value("${experiment.export.local.path.root}")
    private String experimentExportLocalPathRoot;

    @Value("${assignment.file.archive.ttl.days:7}")
    private int ttl;

    @Override
    public Optional<ExperimentImportScheduleResult> cleanup() {
        List<ExperimentImport> expiredExperimentImports = experimentImportRepository.findAllByUpdatedAtLessThanOrStatusIn(
            Timestamp.from(Instant.now().minus(Duration.ofDays(ttl))),
            List.of(
                ExperimentImportStatus.COMPLETE_ACKNOWLEDGED,
                ExperimentImportStatus.ERROR_ACKNOWLEDGED
            )
        )
        .stream()
        .filter(experimentImport -> !experimentImport.isDeleted())
        .toList();

        if (CollectionUtils.isEmpty(expiredExperimentImports)) {
            // no expired experiment imports exist; exit
            return Optional.empty();
        }

        Optional<ExperimentImportScheduleResult> results = Optional.of(
            ExperimentImportScheduleResult.builder()
                .processed(
                    expiredExperimentImports.stream()
                        .map(
                            expiredExperimentImport -> {
                                String error = null;
                                boolean deleted = false;

                                try {
                                    deleted = Paths.get(
                                        String.format(
                                            "%s/%s",
                                            experimentExportLocalPathRoot,
                                            expiredExperimentImport.getFileUri()
                                        )
                                    )
                                    .toFile()
                                    .delete();
                                } catch (SecurityException e) {
                                    error = e.getMessage();
                                }

                                if (!deleted) {
                                    error = "Failed to delete file";
                                    expiredExperimentImport.setStatus(ExperimentImportStatus.DELETION_ERROR);
                                }

                                if (deleted) {
                                    expiredExperimentImport.setStatus(ExperimentImportStatus.DELETED);
                                }

                                experimentImportRepository.save(expiredExperimentImport);

                                return ExperimentImportScheduleMessage.builder()
                                    .deletedAt(Timestamp.from(Instant.now()))
                                    .errors(StringUtils.isEmpty(error) ? null : List.of(error))
                                    .fileName(expiredExperimentImport.getFileName())
                                    .fileUri(expiredExperimentImport.getFileUri())
                                    .id(expiredExperimentImport.getId())
                                    .build();
                            }
                        )
                        .toList()
                )
                .build()
        );

        // delete any empty directories
        FileUtils.listFilesAndDirs(
            Paths.get(experimentExportLocalPathRoot).toFile(),
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
                    log.info("Experiment import cleanup deleted empty directory: [{}]", directory.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("Error deleting directory: [{}]", directory.getAbsolutePath(), e);
                }
            }
        );

        return results;
    }

}
