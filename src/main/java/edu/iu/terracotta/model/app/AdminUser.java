package edu.iu.terracotta.model.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.LtiUserEntity;
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

    @OneToOne(optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private LtiUserEntity ltiUserEntity;

    @Column(columnDefinition = "boolean default false")
    private boolean enabled;

}
