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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "lti_link",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"link_key", "context_id"})
    }
)
public class LtiLinkEntity extends BaseEntity {

    @Id
    @Column(name = "link_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long linkId;

    // per LTI 1.3, the resource link 'id' claim must not be more than 255
    // characters in length.
    @Column(name = "link_key", nullable = false, length = 255)
    private String linkKey;

    @Column(length = 4096)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "context_id")
    private LtiContextEntity context;

    @OneToMany(mappedBy = "link", fetch = FetchType.LAZY)
    private Set<LtiResultEntity> results;

    /**
     * @param linkKey the external id for this link
     * @param context the LTI context
     * @param title   OPTIONAL title of this link (null for none)
     */
    public LtiLinkEntity(String linkKey, LtiContextEntity context, String title) {
        if (StringUtils.isBlank(linkKey)) {
            throw new AssertionError();
        }

        if (context == null) {
            throw new AssertionError();
        }

        this.linkKey = linkKey;
        this.context = context;
        this.title = title;
    }

    public String createHtmlFromLink() {
        return "Link Requested:\n" +
                "Link Key:" +
                linkKey +
                "\nLink Title:" +
                title +
                "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LtiLinkEntity that = (LtiLinkEntity) o;

        if (linkId != that.linkId) {
            return false;
        }

        return Objects.equals(linkKey, that.linkKey);
    }

    @Override
    public int hashCode() {
        int result = (int) linkId;
        result = 31 * result + (linkKey != null ? linkKey.hashCode() : 0);

        return result;
    }

}
