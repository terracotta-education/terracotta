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

import lombok.NoArgsConstructor;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.Objects;
import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lti_link")
public class LtiLinkEntity extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long linkId;

    @Basic
    @Column(nullable = false, length = 4096)
    private String linkKey;

    @Basic
    @Column(length = 4096)
    private String title;

    @JoinColumn(name = "context_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
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
