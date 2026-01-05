package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "lti_result")
public class LtiResultEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "result_id",
        nullable = false
    )
    private long resultId;

    @Column(length = 4096)
    private String comment;

    @Column(nullable = false)
    private Timestamp timestamp;

    @Column private Float scoreGiven;
    @Column private Float scoreMaximum;
    @Column private String activityProgress;
    @Column private String gradingProgress;

    @JoinColumn(name = "link_id")
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    private LtiLinkEntity link;

    @JoinColumn(name = "user_id")
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
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
