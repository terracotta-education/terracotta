package edu.iu.terracotta.connectors.generic.dao.model.lms;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsAssignment;
import edu.iu.terracotta.connectors.generic.dao.model.lms.base.LmsExternalToolFields;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Getter
@Setter
@SuperBuilder
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsAssignment implements BaseLmsAssignment {

    @Builder.Default private int allowedAttempts = -1;
    @Builder.Default private boolean canSubmit = true;

    private Class<?> type;
    private String id;
    private String name;
    private boolean published;
    private String secureParams;
    private Date dueAt;
    private List<String> submissionTypes;
    private Float pointsPossible;
    private Date lockAt;
    private Date unlockAt;
    private LmsExternalToolFields lmsExternalToolFields;
    private String gradingType;
    private String metadata; // JSON metadata from the LMS

    @Override
    public String addMetadata(String key, Map<String, Object> values) throws TerracottaConnectorException {
        if (StringUtils.isBlank(key) || MapUtils.isEmpty(values)) {
            return this.metadata;
        }

        try {
            JsonMapper jsonMapper = JsonMapper.builder().build();
            Map<String, Map<String, Object>> existingMetadata = StringUtils.isNotBlank(metadata) ?
                jsonMapper.readValue(this.metadata, new TypeReference<Map<String, Map<String, Object>>>(){})
                :
                new HashMap<>();

            if (MapUtils.isEmpty(existingMetadata)) {
                this.metadata = jsonMapper.writeValueAsString(
                    Map.of(
                        key, values
                    )
                );

                return this.metadata;
            }

            if (existingMetadata.containsKey(key)) {
                existingMetadata.get(key).putAll(values);
            } else {
                existingMetadata.put(key, values);
            }

            this.metadata = jsonMapper.writeValueAsString(existingMetadata);
        } catch (JacksonException e) {
            throw new TerracottaConnectorException(
                String.format(
                    "Error adding metadata for key: [%s] and values: [%s]",
                    key,
                    values.entrySet().stream()
                        .map(
                            entry -> String.format("%s=%s", entry.getKey(), entry.getValue())
                        )
                        .collect(Collectors.joining(", "))
                ),
                e
            );
        }

        return this.metadata;
    }

    @Override
    public LmsAssignment from() {
        return this;
    }

}
