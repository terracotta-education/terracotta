package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileInfoRepository extends JpaRepository<FileInfo, String> {
    Optional<FileInfo> findByFileId(String fileId);

    void deleteByFileId(String fileId);
}