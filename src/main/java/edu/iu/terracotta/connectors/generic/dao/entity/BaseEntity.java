package edu.iu.terracotta.connectors.generic.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity {

    @Column Timestamp createdAt;
    @Column Timestamp updatedAt;

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
