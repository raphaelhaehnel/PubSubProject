package servlets.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AgentConfig(String type, List<String> pubs, List<String> subs) {
    @JsonCreator
    public AgentConfig(
            @JsonProperty(value = "type", required = true) String type,
            @JsonProperty(value = "pubs") List<String> pubs,
            @JsonProperty(value = "subs") List<String> subs) {

        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent is missing required field: 'type'");
        }

        this.type = type;
        this.pubs = pubs != null ? pubs : List.of();
        this.subs = subs != null ? subs : List.of();
    }
}