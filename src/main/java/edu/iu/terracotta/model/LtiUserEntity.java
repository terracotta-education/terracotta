/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.model;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "lti_user",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_key", "key_id"}),
    }
)
public class LtiUserEntity extends BaseEntity {

    public static final String TEST_STUDENT_DISPLAY_NAME = "Test Student";

    @Id
    @Column(name = "user_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userId;

    // per LTI 1.3 and OIDC specifications, the 'sub' claim must not be more than
    // 255 characters in length.
    @Column(name = "user_key", nullable = false, length = 255)
    private String userKey;

    @Column
    private String lmsUserId;

    @Column(name = "displayname", length = 4096)
    private String displayName;

    /**
     * Actual max for emails is 254 chars
     */
    @Column
    private String email;

    @Column(length = 63)
    private String locale;

    @Column
    private Short subscribe;

    @Lob
    @Column
    private String json;

    @Column
    private Timestamp loginAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<LtiResultEntity> results;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id", referencedColumnName = "key_id")
    private PlatformDeployment platformDeployment;

    /**
     * @param userKey user identifier
     * @param loginAt date of user login
     */
    public LtiUserEntity(String userKey, Date loginAt, PlatformDeployment platformDeployment1) {
        if (StringUtils.isBlank(userKey)) {
            throw new AssertionError();
        }

        if (loginAt == null) {
            loginAt = new Date();
        }

        if (platformDeployment1 != null) {
            this.platformDeployment = platformDeployment1;
        }

        this.userKey = userKey;
        this.loginAt = new Timestamp(loginAt.getTime());
    }

    @Transient
    public boolean isTestStudent() {
        return StringUtils.equalsIgnoreCase(displayName, TEST_STUDENT_DISPLAY_NAME);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LtiUserEntity that = (LtiUserEntity) o;

        if (userId != that.userId) {
            return false;
        }

        if (!Objects.equals(email, that.email)) {
            return false;
        }

        return Objects.equals(userKey, that.userKey);
    }

    @Override
    public int hashCode() {
        int result = (int) userId;
        result = 31 * result + (userKey != null ? userKey.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);

        return result;
    }

}
