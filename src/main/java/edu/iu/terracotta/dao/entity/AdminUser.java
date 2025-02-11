package edu.iu.terracotta.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_admin_user")
public class AdminUser extends BaseEntity  {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminUserId;

    @Column(columnDefinition = "boolean default false")
    private boolean enabled;

    @OneToOne(optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private LtiUserEntity ltiUserEntity;

}
