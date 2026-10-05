package edu.iu.terracotta.dao.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.AdminUser;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    AdminUser findByUuid(UUID uuid);

    Optional<AdminUser> findByLtiUserEntity_UserKey(String userKey);

    boolean existsByLtiUserEntity_UserKeyAndEnabledTrue(String userKey);

}
