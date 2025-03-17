package edu.iu.terracotta.dao.repository.distribute;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.distribute.ExperimentImportError;

public interface ExperimentImportErrorRepository extends JpaRepository<ExperimentImportError, Long> {

}
