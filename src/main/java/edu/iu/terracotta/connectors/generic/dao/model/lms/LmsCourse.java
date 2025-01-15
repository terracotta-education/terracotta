package edu.iu.terracotta.connectors.generic.dao.model.lms;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsCourse;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsCourse implements BaseLmsCourse {

    protected Class<?> type;
    protected Long id;

    @Override
    public LmsCourse convert() {
        return this;
    }

}
