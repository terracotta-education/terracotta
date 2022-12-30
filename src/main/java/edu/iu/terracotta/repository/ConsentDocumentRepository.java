package edu.iu.terracotta.repository;

import edu.iu.terracotta.model.app.ConsentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentDocumentRepository extends JpaRepository<ConsentDocument, Long> {
}