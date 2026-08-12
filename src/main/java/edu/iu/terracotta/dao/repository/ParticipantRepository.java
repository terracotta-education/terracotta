package edu.iu.terracotta.dao.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.iu.terracotta.dao.entity.Participant;
import edu.iu.terracotta.dao.entity.projection.GroupParticipantCount;
import edu.iu.terracotta.dao.entity.projection.LmsParticipantSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByUuid(UUID uuid);
    List<Participant> findByExperiment_ExperimentId(Long experimentId);
    long countByExperiment_ExperimentId(Long experimentId);
    List<Participant> findByExperiment_ExperimentId(Long experimentId, Pageable pageable);
    Optional<Participant> findByIdAndExperiment_ExperimentId(Long id, Long experimentId);
    List<Participant> findByExperiment_ExperimentIdAndGroup_GroupId(Long experimentId, Long groupId);
    Participant findByExperiment_ExperimentIdAndLtiUserEntity_UserKey(Long experimentId, String userKey);
    List<Participant> findAllByExperiment_ExperimentIdAndLtiUserEntity_UserKeyIn(Long experimentId, List<String> userKey);
    boolean existsByExperiment_ExperimentIdAndId(Long experimentId, Long id);
    List<Participant> findByGroup_GroupId(Long groupId);
    long countDistinctByGroup_GroupId(Long groupId);
    long countByGroup_GroupId(Long groupId);

    @Query("SELECT p.group.groupId AS groupId, COUNT(p) AS participantCount FROM Participant p WHERE p.experiment.experimentId = :experimentId AND p.group IS NOT NULL GROUP BY p.group.groupId")
    List<GroupParticipantCount> countByExperiment_ExperimentIdGroupByGroup(@Param("experimentId") Long experimentId);

    @NativeQuery(
        value = """
            SELECT
                MIN(p.id) AS id,
                lu.email AS email
            FROM
                terr_participant p
            JOIN lti_membership lme
                ON p.lti_membership_entity_membership_id = lme.membership_id
            JOIN lti_user lu
                ON p.lti_user_entity_user_id = lu.user_id
            WHERE
                lme.context_id = :contextId AND
                NULLIF(TRIM(lu.lms_user_id), '') IS NULL
            GROUP BY lu.email
            ORDER BY id ASC
            """,
        countQuery = """
            SELECT COUNT(*) FROM (
                SELECT MIN(p.id)
                FROM terr_participant p
                JOIN lti_membership lme
                    ON p.lti_membership_entity_membership_id = lme.membership_id
                JOIN lti_user lu
                    ON p.lti_user_entity_user_id = lu.user_id
                WHERE
                    lme.context_id = :contextId AND
                    NULLIF(TRIM(lu.lms_user_id), '') IS NULL
                GROUP BY lu.email
            ) AS cnt
            """
    )
    List<LmsParticipantSummary> findLmsParticipantSummaryToUpdateByContextId(@Param("contextId") long contextId, Pageable pageable);

    // cheap existence check so a full LMS course-membership fetch isn't kicked off (see
    // ParticipantAsyncServiceImpl.updateParticipantData) when nothing is actually missing an LMS
    // user ID for this context. MySQL's EXISTS(...) evaluates to a 1/0 BIGINT, not a real
    // boolean - the JDBC driver hands that back as a Long, so the return type here has to be
    // Long (not boolean/Boolean) or Spring Data's proxy fails trying to cast it directly.
    @NativeQuery(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM terr_participant p
                JOIN lti_membership lme
                    ON p.lti_membership_entity_membership_id = lme.membership_id
                JOIN lti_user lu
                    ON p.lti_user_entity_user_id = lu.user_id
                WHERE
                    lme.context_id = :contextId AND
                    NULLIF(TRIM(lu.lms_user_id), '') IS NULL
            )
            """
    )
    Long existsLmsParticipantSummaryToUpdateByContextId(@Param("contextId") long contextId);

    @NativeQuery(
        value = """
            SELECT
                MIN(p.id) AS id,
                lu.email AS email
            FROM
                terr_participant p
            JOIN lti_membership lme
                ON p.lti_membership_entity_membership_id = lme.membership_id
            JOIN lti_user lu
                ON p.lti_user_entity_user_id = lu.user_id
            WHERE
                p.experiment_id = :experimentId AND
                lu.lms_user_id IS NULL
            GROUP BY lu.email
            ORDER BY id ASC
            """,
        countQuery = """
            SELECT COUNT(*) FROM (
                SELECT MIN(p.id)
                FROM terr_participant p
                JOIN lti_membership lme
                    ON p.lti_membership_entity_membership_id = lme.membership_id
                JOIN lti_user lu
                    ON p.lti_user_entity_user_id = lu.user_id
                WHERE
                    p.experiment_id = :experimentId AND
                    lu.lms_user_id IS NULL
                GROUP BY lu.email
            ) AS cnt
            """
    )
    List<LmsParticipantSummary> findLmsParticipantSummaryToUpdateByExperimentId(@Param("experimentId") long experimentId, Pageable pageable);

}
