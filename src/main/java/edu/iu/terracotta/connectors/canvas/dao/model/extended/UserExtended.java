package edu.iu.terracotta.connectors.canvas.dao.model.extended;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsUser;
import edu.ksu.canvas.annotation.CanvasObject;
import edu.ksu.canvas.model.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@CanvasObject(postKey = "user")
public class UserExtended extends LmsUser {

    @Builder.Default private User user = new User();

    @Override
    public String getId() {
        return String.valueOf(user.getId());
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public LmsUser from() {
        return (LmsUser) this;
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
