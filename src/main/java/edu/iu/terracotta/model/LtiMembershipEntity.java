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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lti_membership", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "context_id" })
})
public class LtiMembershipEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id", nullable = false)
    private long membershipId;

    @Column
    private Integer role;

    @Column
    private Integer roleOverride;

    @JoinColumn(name = "context_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private LtiContextEntity context;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private LtiUserEntity user;

    public LtiMembershipEntity(LtiContextEntity context, LtiUserEntity user, Integer role) {
        if (user == null) {
            throw new AssertionError();
        }

        if (context == null) {
            throw new AssertionError();
        }

        this.user = user;
        this.context = context;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LtiMembershipEntity that = (LtiMembershipEntity) o;

        if (context.getContextId() != that.context.getContextId()) {
            return false;
        }

        if (membershipId != that.membershipId) {
            return false;
        }

        if (user.getUserId() != that.user.getUserId()) {
            return false;
        }

        return Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        int result = (int) membershipId;
        result = 31 * result + (int) context.getContextId();
        result = 31 * result + (int) user.getUserId();
        result = 31 * result + (role != null ? role.hashCode() : 0);

        return result;
    }

}
