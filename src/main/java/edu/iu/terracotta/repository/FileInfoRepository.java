package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileInfoRepository extends JpaRepository<FileInfo, String> {
    List<FileInfo> findByExperiment_ExperimentId(Long experimentId);

    Optional<FileInfo> findByFileId(String fileId);

    FileInfo findByExperiment_ExperimentIdAndFilename(Long experimentId, String filename);

    void deleteByFileId(String fileId);
}