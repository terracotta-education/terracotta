package edu.iu.terracotta.dao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.QuestionMc;

public interface QuestionMcRepository extends JpaRepository<QuestionMc, Long> {

    Optional<QuestionMc> findByQuestionId(long questionId);

}
