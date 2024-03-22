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

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Specialty class which handles the created_at and updated_at fields automatically
 */
@Getter
@Setter
@MappedSuperclass
public class BaseEntity {

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    Timestamp createdAt;

    @Column
    Timestamp updatedAt;

    @Version
    @Column(name = "entity_version")
    int version;

    @PrePersist
    void preCreate() {
        this.createdAt = this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

}
