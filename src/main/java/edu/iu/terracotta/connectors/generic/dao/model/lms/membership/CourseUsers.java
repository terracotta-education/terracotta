package edu.iu.terracotta.connectors.generic.dao.model.lms.membership;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseUsers {

    @Builder.Default private List<CourseUser> courseUserList = new ArrayList<>();

    @JsonProperty("members")
    public List<CourseUser> getCourseUserList() {
        return courseUserList;
    }

}
