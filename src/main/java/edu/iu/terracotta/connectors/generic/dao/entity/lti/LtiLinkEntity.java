package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import org.apache.commons.lang3.StringUtils;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "link_id",
        nullable = false
    )
    private long linkId;

    // per LTI 1.3, the resource link 'id' claim must not be more than 255
    // characters in length.
    @Column(
        name = "link_key",
        nullable = false,
        length = 255
    )
    private String linkKey;

    @Column(length = 4096)
    private String title;

    @JoinColumn(name = "context_id")
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    private LtiContextEntity context;

    @OneToMany(
        mappedBy = "link",
        fetch = FetchType.LAZY
    )
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

}
