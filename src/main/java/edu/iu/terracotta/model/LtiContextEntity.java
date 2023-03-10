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
import javax.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lti_context", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "context_key", "deployment_id" })
})
public class LtiContextEntity extends BaseEntity {

    @Id
    @Column(name = "context_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long contextId;

    // per LTI 1.3, the 'Context.id' claim must not be more than 255 characters
    // in length.
    @Column(name = "context_key", nullable = false, length = 255)
    private String contextKey;

    @Column(length = 4096)
    private String title;

    @Column(length = 4096)
    private String context_memberships_url;

    @Column(length = 4096)
    private String lineitems;

    @Lob
    @Column
    private String json;

    @Lob
    @Column
    private String settings;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deployment_id", referencedColumnName = "deployment_id", nullable = false)
    private ToolDeployment toolDeployment;

    @OneToMany(mappedBy = "context")
    private Set<LtiLinkEntity> links;

    @OneToMany(mappedBy = "context")
    private Set<LtiMembershipEntity> memberships;

    public LtiContextEntity(String contextKey, ToolDeployment toolDeployment, String title, String json) {
        if (!StringUtils.isNotBlank(contextKey)) {
            throw new AssertionError();
        }

        if (toolDeployment == null) {
            throw new AssertionError();
        }

        this.contextKey = contextKey;
        this.toolDeployment = toolDeployment;
        this.title = title;
        this.json = json;
    }

    public LtiContextEntity(String contextKey, ToolDeployment toolDeployment, String title, String contextMembershipsUrl, String lineitems, String json) {
        if (!StringUtils.isNotBlank(contextKey)) {
            throw new AssertionError();
        }

        if (toolDeployment == null) {
            throw new AssertionError();
        }

        this.contextKey = contextKey;
        this.toolDeployment = toolDeployment;
        this.title = title;
        this.context_memberships_url = contextMembershipsUrl;
        this.lineitems = lineitems;
        this.json = json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LtiContextEntity that = (LtiContextEntity) o;

        if (contextId != that.contextId) {
            return false;
        }

        return Objects.equals(contextKey, that.contextKey);
    }

    @Override
    public int hashCode() {
        int result = (int) contextId;
        result = 31 * result + (contextKey != null ? contextKey.hashCode() : 0);

        return result;
    }

}
