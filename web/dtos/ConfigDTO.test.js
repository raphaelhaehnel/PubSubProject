import { describe, it, expect } from 'vitest';
import { ConfigDTO } from './ConfigDTO.js';

describe('ConfigDTO Validation', () => {
  it('should parse a valid JSON configuration successfully', () => {
    const validJson = JSON.stringify({
      agents: [
        { type: "PlusAgent", subs: ["A", "B"], pubs: ["C"] }
      ]
    });
    
    const config = new ConfigDTO(validJson);
    expect(config.agents.length).toBe(1);
    expect(config.agents[0].type).toBe("PlusAgent");
    expect(config.agents[0].subs).toEqual(["A", "B"]);
  });

  it('should throw an error for invalid JSON syntax', () => {
    const badJson = "{ agents: [ }"; // Broken syntax
    expect(() => new ConfigDTO(badJson)).toThrow("Syntax Error: The provided text is not valid JSON.");
  });

  it('should throw an error if the root is an array instead of an object', () => {
    const arrayJson = "[]";
    expect(() => new ConfigDTO(arrayJson)).toThrow("Schema Error: Config must be a JSON object.");
  });

  it('should throw an error if the agents array is missing', () => {
    const missingAgents = JSON.stringify({ wrong_key: [] });
    expect(() => new ConfigDTO(missingAgents)).toThrow("Schema Error: Config must contain an 'agents' array.");
  });

  it('should delegate to AgentDTO and throw on missing agent fields', () => {
    const missingType = JSON.stringify({
      agents: [
        { subs: ["A"], pubs: ["B"] } // Missing 'type'
      ]
    });
    // We use a Regex here to match the dynamic index in your error string
    expect(() => new ConfigDTO(missingType)).toThrow(/Schema Error: Agent at index 0 is missing.*'type'/);
  });
});