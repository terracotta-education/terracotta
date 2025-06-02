package edu.iu.terracotta.connectors.generic.dao.model.lms;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsUser;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsUser implements BaseLmsUser {

    protected Class<?> type;
    protected String email;
    protected String id;

    @Override
    public LmsUser from() {
        return this;
    }

}
