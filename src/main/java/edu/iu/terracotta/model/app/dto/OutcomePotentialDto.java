package edu.iu.terracotta.model.app.dto;

public class OutcomePotentialDto {

    public String name;
    public String type;
    public Integer assignmentId;
    public Double pointsPossible;
    public boolean terracotta;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Integer assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Double getPointsPossible() {
        return pointsPossible;
    }

    public void setPointsPossible(Double pointsPossible) {
        this.pointsPossible = pointsPossible;
    }

    public boolean isTerracotta() {
        return terracotta;
    }

    public void setTerracotta(boolean terracotta) {
        this.terracotta = terracotta;
    }
}
