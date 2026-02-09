package edu.iu.terracotta.connectors.generic.dao.model.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsUser;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsUser implements BaseLmsUser {

    protected Class<?> type;
    protected String email;
    protected String id;

    @Override
    public LmsUser from() {
        return this;
    }

}
