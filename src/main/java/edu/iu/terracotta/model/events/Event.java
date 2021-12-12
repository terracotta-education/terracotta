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

@Table(name = "terr_event")
@Entity
public class Event {
    @Column(name = "event_id", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(name = "caliper_id")
    private String caliperId;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "actor_type")
    private String actorType;

    @Column(name = "platform_deployment")
    private String platform_deployment ;

    @Column(name = "event_type")
    private String type;

    @Column(name = "event_profile")
    private String profile;

    @Column(name = "event_action")
    private String action;

    @Column(name = "event_group")
    private String group;

    @Column(name = "event_time")
    private Timestamp eventTime;

    @Column(name = "object_id")
    private String objectId;

    @Column(name = "objectType")
    private String objectType;

    @Column(name = "generated_id")
    private String generatedId;

    @Column(name = "generated_type")
    private String generatedType;

    @Column(name = "referrer_id")
    private String referrerId;

    @Column(name = "referred_type")
    private String referredType;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "federated_session")
    private String federatedSession;

    @Column(name = "membership_id")
    private String membershipId;

    @Column(name = "membership_roles")
    private String membershipRoles;

    @Column(name = "lti_context_id")
    private String ltiContextId;

    @Column(name = "json")
    @Lob
    private String json;

    @JoinColumn(name = "participant_participant_id", nullable = true)
    @ManyToOne(optional = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
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


    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getCaliperId() {
        return caliperId;
    }

    public void setCaliperId(String caliperId) {
        this.caliperId = caliperId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getPlatform_deployment() {
        return platform_deployment;
    }

    public void setPlatform_deployment(String platform_deployment) {
        this.platform_deployment = platform_deployment;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Timestamp getEventTime() {
        return eventTime;
    }

    public void setEventTime(Timestamp eventTime) {
        this.eventTime = eventTime;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getGeneratedId() {
        return generatedId;
    }

    public void setGeneratedId(String generatedId) {
        this.generatedId = generatedId;
    }

    public String getGeneratedType() {
        return generatedType;
    }

    public void setGeneratedType(String generatedType) {
        this.generatedType = generatedType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getFederatedSession() {
        return federatedSession;
    }

    public void setFederatedSession(String federatedSession) {
        this.federatedSession = federatedSession;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public String getMembershipRoles() {
        return membershipRoles;
    }

    public void setMembershipRoles(String membershipRoles) {
        this.membershipRoles = membershipRoles;
    }

    public String getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(String referrerId) {
        this.referrerId = referrerId;
    }

    public String getReferredType() {
        return referredType;
    }

    public void setReferredType(String referredType) {
        this.referredType = referredType;
    }

    public String getLtiContextId() {
        return ltiContextId;
    }

    public void setLtiContextId(String ltiContextId) {
        this.ltiContextId = ltiContextId;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }


    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }
}
