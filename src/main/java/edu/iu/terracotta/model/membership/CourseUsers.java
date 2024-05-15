package edu.iu.terracotta.model.membership;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseUsers {

    private List<CourseUser> courseUserList = new ArrayList<>();

    @JsonProperty("members")
    public List<CourseUser> getCourseUserList() {
        return courseUserList;
    }

}
