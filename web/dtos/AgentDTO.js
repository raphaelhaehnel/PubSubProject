export class AgentDTO {
  constructor(data, index) {
    if (!data.type || typeof data.type !== 'string') {
      throw new Error(`Schema Error: Agent at index ${index} is missing or has an invalid 'type'.`);
    }
    if (!Array.isArray(data.subs)) {
      throw new Error(`Schema Error: Agent at index ${index} is missing the 'subs' array.`);
    }
    if (!Array.isArray(data.pubs)) {
      throw new Error(`Schema Error: Agent at index ${index} is missing the 'pubs' array.`);
    }

    this.type = data.type;
    this.subs = data.subs;
    this.pubs = data.pubs;
  }
}