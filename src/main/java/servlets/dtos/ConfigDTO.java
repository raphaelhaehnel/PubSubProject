package servlets.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import servlets.dtos.AgentDTO;


public class ConfigDTO {
    private final List<AgentDTO> agents;

    @JsonCreator
    public ConfigDTO(@JsonProperty(value = "agents", required = true) List<AgentDTO> agents) {
        if (agents == null || agents.isEmpty()) {
            throw new IllegalArgumentException("The 'agents' array cannot be missing or empty.");
        }
        this.agents = agents;
    }

    public List<AgentDTO> getAgents() {
        return agents;
    }
}