package edu.iu.terracotta.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.QuestionMc;

public interface QuestionMcRepository extends JpaRepository<QuestionMc, Long> {

}
