package edu.iu.terracotta.connectors.caliper.dao.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import edu.iu.terracotta.connectors.caliper.dao.model.events.CaliperEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * What's sent to a Caliper endpoint, and stored with each event: the events, and the sensor that sent them.
 */
@Getter
@AllArgsConstructor
@JsonPropertyOrder({"sensor", "sendTime", "dataVersion", "data"})
public class Envelope {

    @JsonProperty("sensor") private final String sensorId;
    private final Instant sendTime;
    private final String dataVersion;
    private final List<CaliperEvent> data;

}
