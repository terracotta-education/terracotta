package edu.iu.terracotta.dao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.ObsoleteAssignment;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ObsoleteAssignmentRepository extends JpaRepository<ObsoleteAssignment, Long> {

    List<ObsoleteAssignment> findAllByContext_ContextId(Long contextId);

}
