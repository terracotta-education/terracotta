package edu.iu.terracotta.connectors.brightspace.dao.model.extended;

import edu.iu.terracotta.connectors.brightspace.io.model.ClasslistUser;
import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class UserExtended extends LmsUser {

    @Builder.Default private ClasslistUser classlistUser = ClasslistUser.builder().build();

    @Override
    public String getId() {
        return String.valueOf(classlistUser.getIdentifier());
    }

    @Override
    public String getEmail() {
        return classlistUser.getEmail();
    }

    @Override
    public LmsUser from() {
        LmsUser lmsUser = LmsUser.builder().build();

        lmsUser.setId(this.getId());
        lmsUser.setEmail(this.getEmail());

        return lmsUser;
    }

    public static UserExtended of(LmsUser lmsUser) {
        if (lmsUser == null) {
            return UserExtended.builder().build();
        }

        UserExtended userExtended = UserExtended.builder().build();
        userExtended.setId(lmsUser.getId());
        userExtended.setEmail(lmsUser.getEmail());

        return userExtended;
    }

}
