package edu.iu.terracotta.connectors.generic.dao.model.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsCourse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsCourse implements BaseLmsCourse {

    private Class<?> type;
    private String id;

    @Override
    public LmsCourse from() {
        return this;
    }

}
