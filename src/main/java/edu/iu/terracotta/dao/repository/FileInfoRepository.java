package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.FileInfo;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface FileInfoRepository extends JpaRepository<FileInfo, String> {

    List<FileInfo> findByExperiment_ExperimentId(Long experimentId);
    Optional<FileInfo> findByFileId(String fileId);
    FileInfo findByExperiment_ExperimentIdAndFilename(Long experimentId, String filename);
    void deleteByFileId(String fileId);

}
