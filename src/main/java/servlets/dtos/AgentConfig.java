package servlets.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Jackson DTO for a single agent entry in an uploaded configuration.
 * <p>
 * Used to strictly validate the {@code POST /upload} payload before the graph is built:
 * {@code type} is mandatory, while {@code pubs} and {@code subs} default to empty lists
 * when omitted.
 *
 * @param type the agent class name (required, non-blank)
 * @param pubs the topics this agent publishes to (never {@code null})
 * @param subs the topics this agent subscribes to (never {@code null})
 */
record AgentConfig(String type, List<String> pubs, List<String> subs) {
    /**
     * Canonical constructor invoked by Jackson during deserialization.
     *
     * @param type the agent class name; must be present and non-blank
     * @param pubs the published topics, or {@code null} (normalized to an empty list)
     * @param subs the subscribed topics, or {@code null} (normalized to an empty list)
     * @throws IllegalArgumentException if {@code type} is missing or blank
     */
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