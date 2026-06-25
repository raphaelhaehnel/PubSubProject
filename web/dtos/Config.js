import { AgentDTO } from './Agent.js';

export class ConfigDTO {
  constructor(jsonString) {
    let parsed;
    try {
      parsed = JSON.parse(jsonString);
    } catch (err) {
      throw new Error("Syntax Error: The provided text is not valid JSON.");
    }

    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error("Schema Error: Config must be a JSON object.");
    }
    
    if (!Array.isArray(parsed.agents)) {
      throw new Error("Schema Error: Config must contain an 'agents' array.");
    }

    this.agents = parsed.agents.map((agentData, index) => new AgentDTO(agentData, index));
  }
}