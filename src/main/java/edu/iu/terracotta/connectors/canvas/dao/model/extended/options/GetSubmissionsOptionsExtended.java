package edu.iu.terracotta.connectors.canvas.dao.model.extended.options;

import java.util.List;
import java.util.Locale;

import edu.ksu.canvas.requestOptions.GetSubmissionsOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("PMD.LambdaCanBeMethodReference")
public class GetSubmissionsOptionsExtended extends GetSubmissionsOptions {

    private List<String> assignmentIds;

    public enum UserId {
        ALL;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.US);
        }
    }

    public GetSubmissionsOptionsExtended(String canvasId, List<String> assignmentIds) {
        super(canvasId);
        assignmentIds(assignmentIds);
    }

    public GetSubmissionsOptions assignmentIds(List<String> assignmentIds) {
        this.assignmentIds = assignmentIds;
        addStringList("assignment_ids[]", assignmentIds);

        return this;
    }

}
