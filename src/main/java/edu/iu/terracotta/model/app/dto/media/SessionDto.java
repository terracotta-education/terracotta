package edu.iu.terracotta.model.app.dto.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionDto extends AbstractDto {
    private String type;
    private DateTime startedAtTime;

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public DateTime getStartedAtTime() {
      return startedAtTime;
   }

   public void setStartedAtTime(DateTime startedAtTime) {
      this.startedAtTime = startedAtTime;
   }
}
