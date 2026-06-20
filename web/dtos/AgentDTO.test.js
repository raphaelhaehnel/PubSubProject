import { describe, it, expect } from 'vitest';
import { AgentDTO } from './AgentDTO.js';

describe('AgentDTO Validation', () => {
  it('should create a valid AgentDTO when all fields are present', () => {
    const data = { type: "PlusAgent", subs: ["A"], pubs: ["B"] };
    const agent = new AgentDTO(data, 0);
    expect(agent.type).toBe("PlusAgent");
  });

  it('should throw an error if "type" is missing', () => {
    const data = { subs: ["A"], pubs: ["B"] };
    expect(() => new AgentDTO(data, 0)).toThrow(/missing or has an invalid 'type'/);
  });

  it('should throw an error if "subs" is not an array', () => {
    const data = { type: "PlusAgent", subs: "NotAnArray", pubs: ["B"] };
    expect(() => new AgentDTO(data, 0)).toThrow(/missing the 'subs' array/);
  });
});