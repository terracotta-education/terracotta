package edu.iu.terracotta.model.app;

import java.util.List;

import edu.iu.terracotta.model.BaseEntity;
import edu.iu.terracotta.model.PlatformDeployment;
import edu.iu.terracotta.model.app.enumerator.FeatureType;
import edu.iu.terracotta.model.canvas.CanvasAPIScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_feature")
public class Feature extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private FeatureType type;

    @Column
    private boolean enabled;

    @ManyToMany(mappedBy = "features")
    private List<CanvasAPIScope> scopes;

    @ManyToMany
    private List<PlatformDeployment> platformDeployments;

}
