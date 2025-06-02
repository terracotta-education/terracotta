package edu.iu.terracotta.dao.entity.events;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import edu.iu.terracotta.dao.entity.Participant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "terr_event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "event_id",
        nullable = false
    )
    private Long eventId;

    @Column private String caliperId;
    @Column private String actorId;
    @Column private String actorType;
    @Column private String platform_deployment;
    @Column private Timestamp eventTime;
    @Column private String objectId;
    @Column private String objectType;
    @Column private String generatedId;
    @Column private String generatedType;
    @Column private String referrerId;
    @Column private String referredType;
    @Column private String targetId;
    @Column private String targetType;
    @Column private String federatedSession;
    @Column private String membershipId;
    @Column private String membershipRoles;
    @Column private String ltiContextId;

    @Column(name = "event_type")
    private String type;

    @Column(name = "event_profile")
    private String profile;

    @Column(name = "event_action")
    private String action;

    @Column(name = "event_group")
    private String group;

    @Lob
    @Column
    private String json;

    @ManyToOne(optional = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "participant_id", nullable = true)
    private Participant participant;

    /*
        The profile/action
            AssessmentEvent.Started  <-- When the user starts and assignment and creates a new submission
            AssessmentEvent.Restarted  <--when the user access to an assignment and reuses an existing submission
            AssessmentEvent.Submitted <--When the user clicks on the submit button (or the system automatically submits)
            NavigationEvent.NavigatedTo <-- Surely only when the student goes to the dashboard and some other options (Not in the POC except the dashborard)
            FeedbackEvent.Commented  <-- (not in POC?)
            GradeEvent.Graded <-- This is not a student event.... so no.
            ViewEvent.Viewed <-- If the student views the grading.
            ToolUseEvent.Used  <-- Every time a student launches the tool (dashboard or assignment, does not matters)
    */

}
