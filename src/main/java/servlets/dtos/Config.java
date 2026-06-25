package servlets.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public record Config(List<AgentConfig> agents) {
    @JsonCreator
    public Config(@JsonProperty(value = "agents", required = true) List<AgentConfig> agents) {
        if (agents == null || agents.isEmpty()) {
            throw new IllegalArgumentException("The 'agents' array cannot be missing or empty.");
        }
        this.agents = agents;
    }
}