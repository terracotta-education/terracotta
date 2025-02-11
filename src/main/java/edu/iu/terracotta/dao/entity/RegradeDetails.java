package edu.iu.terracotta.dao.entity;

import java.util.ArrayList;
import java.util.List;

import edu.iu.terracotta.dao.model.enums.RegradeOption;
import lombok.Setter;

@Setter
public class RegradeDetails {

    private RegradeOption regradeOption;
    private List<Long> editedMCQuestionIds;

    /**
     * Retrieve the regrade option, or {@link RegradeOption.NA} if null
     *
     * @return
     */
    public RegradeOption getRegradeOption() {
        if (regradeOption == null) {
            return RegradeOption.NA;
        }

        return regradeOption;
    }

    /**
     * Retrieve the MC question IDs to regrade, or an empty list if null
     *
     * @return
     */
    public List<Long> getEditedMCQuestionIds() {
        if (editedMCQuestionIds == null) {
            return new ArrayList<>();
        }

        return editedMCQuestionIds;
    }

}
