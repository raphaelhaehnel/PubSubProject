package servlets.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


/**
 * Jackson DTO for a whole uploaded configuration: a wrapper around the list of
 * {@link AgentConfig} entries found under the JSON {@code "agents"} field.
 *
 * @param agents the agents to instantiate (required, never {@code null})
 */
public record Config(List<AgentConfig> agents) {
    /**
     * Canonical constructor invoked by Jackson during deserialization.
     *
     * @param agents the list of agent entries; must be present
     * @throws IllegalArgumentException if the {@code agents} array is missing
     */
    @JsonCreator
    public Config(@JsonProperty(value = "agents", required = true) List<AgentConfig> agents) {
        if (agents == null) {
            throw new IllegalArgumentException("The 'agents' array cannot be missing.");
        }
        this.agents = agents;
    }
}