package edu.iu.terracotta.model.app;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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
