package edu.iu.terracotta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.app.QuestionMc;

public interface QuestionMcRepository extends JpaRepository<QuestionMc, Long> {

}
