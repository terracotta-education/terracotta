package edu.iu.terracotta.runner.assignmentfilearchive;

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

import edu.iu.terracotta.dao.entity.AssignmentFileArchive;
import edu.iu.terracotta.dao.model.enums.AssignmentFileArchiveStatus;
import edu.iu.terracotta.dao.repository.AssignmentFileArchiveRepository;
import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleMessage;
import edu.iu.terracotta.runner.assignmentfilearchive.model.AssignmentFileArchiveScheduleResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class AssignmentFileArchiveSchedulerServiceImpl implements AssignmentFileArchiveSchedulerService {

    @Autowired private AssignmentFileArchiveRepository assignmentFileArchiveRepository;

    @Value("${assignment.file.archive.local.path.root}")
    private String assignmentFileArchiveLocalPathRoot;

    @Value("${assignment.file.archive.ttl.days:7}")
    private int ttl;

    @Override
    public Optional<AssignmentFileArchiveScheduleResult> cleanup() {
        List<AssignmentFileArchive> expiredAssignmentFileArchives = assignmentFileArchiveRepository.findAllByUpdatedAtLessThanAndStatusIn(
            Timestamp.from(Instant.now().minus(Duration.ofDays(ttl))),
            List.of(
                AssignmentFileArchiveStatus.DOWNLOADED,
                AssignmentFileArchiveStatus.ERROR,
                AssignmentFileArchiveStatus.ERROR_ACKNOWLEDGED,
                AssignmentFileArchiveStatus.OUTDATED,
                AssignmentFileArchiveStatus.READY
            )
        );

        if (CollectionUtils.isEmpty(expiredAssignmentFileArchives)) {
            // no expired file archives exist; exit
            return Optional.empty();
        }

        Optional<AssignmentFileArchiveScheduleResult> results = Optional.of(
            AssignmentFileArchiveScheduleResult.builder()
                .processed(
                    expiredAssignmentFileArchives.stream()
                        .map(
                            expiredAssignmentFileArchive -> {
                                String error = null;
                                boolean deleted = false;
                                try {
                                    deleted = Paths.get(
                                        String.format(
                                            "%s/%s%s",
                                            assignmentFileArchiveLocalPathRoot,
                                            expiredAssignmentFileArchive.getFileUri(),
                                            AssignmentFileArchive.COMPRESSED_FILE_EXTENSION
                                        )
                                    )
                                    .toFile()
                                    .delete();
                                } catch (SecurityException e) {
                                    error = e.getMessage();
                                }

                                if (!deleted) {
                                    error = "Failed to delete file";
                                }

                                expiredAssignmentFileArchive.setStatus(AssignmentFileArchiveStatus.DELETED);
                                assignmentFileArchiveRepository.save(expiredAssignmentFileArchive);

                                return AssignmentFileArchiveScheduleMessage.builder()
                                    .deletedAt(Timestamp.from(Instant.now()))
                                    .errors(StringUtils.isEmpty(error) ? null : List.of(error))
                                    .fileName(expiredAssignmentFileArchive.getFileName())
                                    .fileUri(expiredAssignmentFileArchive.getFileUri())
                                    .id(expiredAssignmentFileArchive.getId())
                                    .build();
                            }
                        )
                        .toList()
                )
                .build()
        );

        // delete any empty directories
        FileUtils.listFilesAndDirs(
            Paths.get(assignmentFileArchiveLocalPathRoot).toFile(),
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
                    log.info("Assignment file archive cleanup deleted empty directory: [{}]", directory.getAbsolutePath());
                } catch (IOException e) {
                    log.warn("Error deleting directory: [{}]", directory.getAbsolutePath(), e);
                }
            }
        );

        return results;
    }

}
