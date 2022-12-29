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


import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Date;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lti_result")
public class LtiResultEntity extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long resultId;

    @Basic
    @Column
    private Float scoreGiven;

    @Basic
    @Column
    private Float scoreMaximum;

    @Basic
    @Column(length = 4096)
    private String comment;

    @Basic
    @Column
    private String activityProgress;

    @Basic
    @Column
    private String gradingProgress;

    @Basic
    @Column(nullable = false)
    private Timestamp timestamp;

    @JoinColumn(name = "link_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private LtiLinkEntity link;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private LtiUserEntity user;

    /**
     * @param user        the user for this grade result
     * @param link        the link which this is a grade for
     * @param retrievedAt the date the grade was retrieved (null indicates now)
     * @param scoreGiven       [OPTIONAL] the grade value
     */
    public LtiResultEntity(LtiUserEntity user, LtiLinkEntity link, Date retrievedAt, Float scoreGiven, Float scoreMaximum, String comment, String activityProgress, String gradingProgress) {
        if (user == null) {
            throw new AssertionError();
        }

        if (link == null) {
            throw new AssertionError();
        }

        if (retrievedAt == null) {
            retrievedAt = new Date();
        }

        this.timestamp = new Timestamp(retrievedAt.getTime());
        this.user = user;
        this.link = link;
        this.scoreGiven = scoreGiven;
        this.scoreMaximum = scoreMaximum;
        this.comment = comment;
        this.comment = activityProgress;
        this.comment = gradingProgress;
    }

}
