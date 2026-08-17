package edu.iu.terracotta.dao.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.iu.Terracotta;

/**
 * Boots the real Spring/Hibernate context (rather than a Mockito-mocked repository) to verify
 * that {@link ParticipantRepository#findLmsParticipantSummaryToUpdateByContextId} is preparable
 * by Hibernate against MySQL-shaped LIMIT/OFFSET syntax. A prior version of this native query
 * bound LIMIT/OFFSET via named parameters ({@code LIMIT :limit OFFSET :offset}), which Hibernate
 * rejects at query-preparation time ("no viable alternative at input") - the query never reaches
 * the database, so no test data is needed to reproduce or verify the fix; only preparation and
 * execution against an empty schema matters.
 */
@SpringBootTest(
    classes = Terracotta.class,
    properties = {
        "aws.enabled=false",
        // isolated in-memory H2 instance, overriding any ambient/profile-based datasource
        "spring.datasource.url=jdbc:h2:mem:participant-repository-lms-summary-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class ParticipantRepositoryLmsSummaryQueryTest {

    @Autowired private ParticipantRepository participantRepository;

    @Test
    void findLmsParticipantSummaryToUpdateByContextIdPreparesAndExecutes() {
        assertDoesNotThrow(() -> participantRepository.findLmsParticipantSummaryToUpdateByContextId(1L, 10, 0L));
    }

}
