package edu.iu.terracotta.dao.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.AnswerMcSubmissionOption;


public interface AnswerMcSubmissionOptionRepository extends JpaRepository<AnswerMcSubmissionOption, Long> {

    AnswerMcSubmissionOption findByUuid(UUID uuid);

}
