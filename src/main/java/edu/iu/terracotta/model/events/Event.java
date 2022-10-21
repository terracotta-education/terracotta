package edu.iu.terracotta.model.events;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import edu.iu.terracotta.model.app.Participant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_event")
public class Event {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column
    private String caliperId;

    @Column
    private String actorId;

    @Column
    private String actorType;

    @Column
    private String platformDeployment;

    @Column
    private String eventType;

    @Column
    private String eventProfile;

    @Column
    private String eventAction;

    @Column
    private String eventGroup;

    @Column
    private Timestamp eventTime;

    @Column
    private String objectId;

    @Column
    private String objectType;

    @Column
    private String generatedId;

    @Column
    private String generatedType;

    @Column
    private String referrerId;

    @Column
    private String referredType;

    @Column
    private String targetId;

    @Column
    private String targetType;

    @Column
    private String federatedSession;

    @Column
    private String membershipId;

    @Column
    private String membershipRoles;

    @Column
    private String ltiContextId;

    @Lob
    @Column
    private String json;

    @ManyToOne(optional = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "participant_participant_id")
    private Participant participant;


    // The profile/action
        //AssessmentEvent.Started  <-- When the user starts and assignment and creates a new submission
        //AssessmentEvent.Restarted  <--when the user access to an assignment and reuses an existing submission
        //AssessmentEvent.Submitted <--When the user clicks on the submit button (or the system automatically submits)
        //NavigationEvent.NavigatedTo <-- Surely only when the student goes to the dashboard and some other options (Not in the POC except the dashborard)
        //FeedbackEvent.Commented  <-- (not in POC?)
        //GradeEvent.Graded <-- This is not a student event.... so no.
        //ViewEvent.Viewed <-- If the student views the grading.
        //ToolUseEvent.Used  <-- Every time a student launches the tool (dashboard or assignment, does not matters)

}
