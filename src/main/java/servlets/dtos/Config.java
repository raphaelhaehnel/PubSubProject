package servlets.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import servlets.dtos.Agent;


public class Config {
    private final List<Agent> agents;

    @JsonCreator
    public Config(@JsonProperty(value = "agents", required = true) List<Agent> agents) {
        if (agents == null || agents.isEmpty()) {
            throw new IllegalArgumentException("The 'agents' array cannot be missing or empty.");
        }
        this.agents = agents;
    }

    public List<Agent> getAgents() {
        return agents;
    }
}