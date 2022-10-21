package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.imsglobal.caliper.entities.EntityType;
import org.imsglobal.caliper.entities.agent.Status;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MembershipDto extends AbstractDto {

         private EntityType   type;
         private String   member;
         private String organization;
         private String[] roles;
         private Status active;
         private DateTime dateCreated;


    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public Status getActive() {
        return active;
    }

    public void setActive(Status active) {
        this.active = active;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(DateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
