package edu.iu.terracotta.connectors.brightspace.io.net.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Response {

    private int responseCode;
    private String next;
    private String content;

    @Builder.Default private boolean errorHappened = false;

    @Override
    public String toString() {
        return "Response{" +
                "errorHappened=" + errorHappened +
                ", responseCode=" + responseCode +
                ", next='" + next + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
