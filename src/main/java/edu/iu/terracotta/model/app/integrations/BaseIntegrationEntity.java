package edu.iu.terracotta.model.app.integrations;

import java.util.UUID;

import edu.iu.terracotta.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class BaseIntegrationEntity extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(nullable = false)
    protected UUID uuid;

    @PrePersist
    protected void prePersist() {
        if (uuid != null) {
            return;
        }

        uuid = UUID.randomUUID();
    }

}