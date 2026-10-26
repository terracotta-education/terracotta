package edu.iu.terracotta.dao.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import edu.iu.Terracotta;

/**
 * Boots the real Spring/Hibernate context to verify that
 * {@link AnswerFileSubmissionRepository#findLatestUploadTimeByAssignmentId} walks the
 * submission -> question -> assessment -> treatment -> assignment path correctly and returns the
 * newest upload, from any participant: the file archive includes everyone's files, consenting or
 * not, so anyone's upload makes it outdated.
 */
@SpringBootTest(
    classes = Terracotta.class,
    properties = {
        "aws.enabled=false",
        // isolated in-memory H2 instance, overriding any ambient/profile-based datasource
        "spring.datasource.url=jdbc:h2:mem:answer-file-submission-latest-upload-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=sa",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    }
)
@ActiveProfiles("test")
class AnswerFileSubmissionRepositoryLatestUploadQueryTest {

    @Autowired private AnswerFileSubmissionRepository answerFileSubmissionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        // only the rows on the query's join path matter, so skip the unrelated parents
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        // H2 commits on SET REFERENTIAL_INTEGRITY, so a test transaction can't roll the seed back
        for (String table : new String[] {"terr_answer_file_submission", "terr_question_submission", "terr_submission",
                "terr_participant", "terr_question", "terr_assessment", "terr_treatment", "terr_assignment"}) {
            jdbcTemplate.execute("DELETE FROM " + table);
        }

        for (long id = 1; id <= 2; id++) {
            jdbcTemplate.update("""
                INSERT INTO terr_assignment (assignment_id, exposure_exposure_id, allow_student_view_correct_answers,
                    allow_student_view_responses, multiple_submission_scoring_scheme, uuid, entity_version, created_at)
                VALUES (?, 1, false, false, 'MOST_RECENT', RANDOM_UUID(), 0, CURRENT_TIMESTAMP)
                """, id);
            jdbcTemplate.update("""
                INSERT INTO terr_treatment (treatment_id, assignment_assignment_id, condition_condition_id, assessment_assessment_id,
                    uuid, entity_version, created_at)
                VALUES (?, ?, 1, ?, RANDOM_UUID(), 0, CURRENT_TIMESTAMP)
                """, id, id, id);
            jdbcTemplate.update("""
                INSERT INTO terr_assessment (assessment_id, treatment_treatment_id, allow_student_view_correct_answers,
                    allow_student_view_responses, auto_submit, multiple_submission_scoring_scheme, uuid, entity_version, created_at)
                VALUES (?, ?, false, false, false, 'MOST_RECENT', RANDOM_UUID(), 0, CURRENT_TIMESTAMP)
                """, id, id);
            jdbcTemplate.update("""
                INSERT INTO terr_question (question_id, assessment_assessment_id, uuid, entity_version, created_at)
                VALUES (?, ?, RANDOM_UUID(), 0, CURRENT_TIMESTAMP)
                """, id, id);
        }

        insertParticipant(1, true);
        insertParticipant(2, false);
        insertParticipant(3, null);

        // submission, assessment/question, participant
        insertSubmission(1, 1, 1);
        insertSubmission(2, 1, 2);
        insertSubmission(3, 1, 3);
        insertSubmission(4, 2, 1);

        insertUpload(1, 1, "2026-01-01 00:00:00");
        insertUpload(2, 1, "2026-02-01 00:00:00");
        // newer, from participants who haven't consented (false) or haven't answered (null)
        insertUpload(3, 2, "2026-03-01 00:00:00");
        insertUpload(4, 3, "2026-03-15 00:00:00");
        // newer, but for the other assignment
        insertUpload(5, 4, "2026-04-01 00:00:00");

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void returnsTheNewestUploadWhetherOrNotTheParticipantConsented() {
        Optional<Timestamp> latest = answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(1L);

        assertTrue(latest.isPresent());
        assertEquals(Timestamp.valueOf("2026-03-15 00:00:00"), latest.get());
    }

    @Test
    void scopesUploadsToTheAssignment() {
        Optional<Timestamp> latest = answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(2L);

        assertTrue(latest.isPresent());
        assertEquals(Timestamp.valueOf("2026-04-01 00:00:00"), latest.get());
    }

    @Test
    void returnsEmptyWithoutUploads() {
        assertTrue(answerFileSubmissionRepository.findLatestUploadTimeByAssignmentId(999L).isEmpty());
    }

    private void insertParticipant(long id, Boolean consent) {
        jdbcTemplate.update("""
            INSERT INTO terr_participant (id, experiment_experiment_id, lti_membership_entity_membership_id, lti_user_entity_user_id,
                consent, uuid, entity_version, created_at)
            VALUES (?, 1, ?, ?, ?, RANDOM_UUID(), 0, CURRENT_TIMESTAMP)
            """, id, id, id, consent);
    }

    private void insertSubmission(long id, long assessmentAndQuestionId, long participantId) {
        jdbcTemplate.update("""
            INSERT INTO terr_submission (submission_id, assessment_assessment_id, participant_id, grade_overridden, late_submission,
                uuid, entity_version, created_at)
            VALUES (?, ?, ?, false, false, RANDOM_UUID(), 0, CURRENT_TIMESTAMP)
            """, id, assessmentAndQuestionId, participantId);
        jdbcTemplate.update("""
            INSERT INTO terr_question_submission (question_submission_id, question_question_id, submission_submission_id,
                uuid, entity_version, created_at)
            VALUES (?, ?, ?, RANDOM_UUID(), 0, CURRENT_TIMESTAMP)
            """, id, assessmentAndQuestionId, id);
    }

    private void insertUpload(long id, long questionSubmissionId, String updatedAt) {
        jdbcTemplate.update("""
            INSERT INTO terr_answer_file_submission (answer_file_submission_id, quest_sub_quest_sub_id, uuid, entity_version,
                created_at, updated_at)
            VALUES (?, ?, RANDOM_UUID(), 0, CURRENT_TIMESTAMP, ?)
            """, id, questionSubmissionId, Timestamp.valueOf(updatedAt));
    }

}
