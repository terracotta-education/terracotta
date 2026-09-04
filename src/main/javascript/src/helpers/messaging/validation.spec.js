import { describe, it, expect } from "vitest";

import {
  initValidations,
  validateConditionalText,
  validateContainer,
  validateMessage
} from "./validation";

describe("initValidations", () => {
  it("returns a fresh, error-free validations object", () => {
    const result = initValidations();

    expect(result.hasErrors).toBe(false);
    expect(result.container.hasErrors).toBe(false);
    expect(result.message.hasErrors).toBe(false);
    expect(result.message.recipients.ruleSets).toEqual([]);
    expect(result.message.conditionalText.ruleSets).toEqual([]);
  });

  it("returns a new deep copy every time (mutating one result does not affect another)", () => {
    const first = initValidations();
    first.hasErrors = true;
    first.container.title = "mutated";

    const second = initValidations();

    expect(second.hasErrors).toBe(false);
    expect(second.container.title).toBeNull();
  });
});

describe("validateContainer", () => {
  function buildContainer(overrides = {}) {
    return {
      configuration: {
        title: "My Container",
        type: "EMAIL",
        sendAt: "2026-08-01T10:00:00.000Z",
        ...overrides
      }
    };
  }

  it("passes validation with a complete, valid container", () => {
    const result = validateContainer(buildContainer());

    expect(result.hasErrors).toBe(false);
    expect(result.title).toBeNull();
    expect(result.type).toBeNull();
    expect(result.sendAt.date).toBeNull();
    expect(result.sendAt.time).toBeNull();
  });

  it("flags a missing title", () => {
    const result = validateContainer(buildContainer({ title: "" }));

    expect(result.hasErrors).toBe(true);
    expect(result.title).toBe("Title is required.");
  });

  it("flags a title that is only whitespace", () => {
    const result = validateContainer(buildContainer({ title: "   " }));

    expect(result.hasErrors).toBe(true);
    expect(result.title).toBe("Title is required.");
  });

  it("flags a missing message type", () => {
    const result = validateContainer(buildContainer({ type: null }));

    expect(result.hasErrors).toBe(true);
    expect(result.type).toBe("Message type is required.");
  });

  it("flags a message type of NONE", () => {
    const result = validateContainer(buildContainer({ type: "NONE" }));

    expect(result.hasErrors).toBe(true);
    expect(result.type).toBe("Message type is required.");
  });

  it("flags a missing sendAt", () => {
    const result = validateContainer(buildContainer({ sendAt: null }));

    expect(result.hasErrors).toBe(true);
    expect(result.sendAt.date).toBe("Schedule date is required.");
    expect(result.sendAt.time).toBe("Schedule time is required.");
  });

  it("flags an invalid (unparseable) sendAt", () => {
    const result = validateContainer(
      buildContainer({ sendAt: "not-a-real-date" })
    );

    expect(result.hasErrors).toBe(true);
    expect(result.sendAt.date).toBe("Schedule date is required.");
    expect(result.sendAt.time).toBe("Schedule time is required.");
  });

  it("accumulates multiple errors at once", () => {
    const result = validateContainer(
      buildContainer({ title: "", type: "NONE", sendAt: null })
    );

    expect(result.hasErrors).toBe(true);
    expect(result.title).toBe("Title is required.");
    expect(result.type).toBe("Message type is required.");
    expect(result.sendAt.date).toBe("Schedule date is required.");
  });
});

describe("validateConditionalText", () => {
  function buildConditionalText(overrides = {}) {
    return {
      id: 1,
      label: "My Label",
      result: { html: "<p>content</p>" },
      ruleSets: [],
      ...overrides
    };
  }

  it("passes validation with a valid, unique label and result content", () => {
    const result = validateConditionalText([], buildConditionalText());

    expect(result.hasErrors).toBe(false);
    expect(result.label).toBeNull();
    expect(result.result).toBeNull();
  });

  it("flags a missing label", () => {
    const result = validateConditionalText([], buildConditionalText({ label: "" }));

    expect(result.hasErrors).toBe(true);
    expect(result.label).toBe("Label is required.");
  });

  it("flags a label that is only whitespace", () => {
    const result = validateConditionalText(
      [],
      buildConditionalText({ label: "   " })
    );

    expect(result.hasErrors).toBe(true);
    expect(result.label).toBe("Label is required.");
  });

  it("flags a duplicate label against other conditional texts", () => {
    const existing = [
      { id: 2, label: "My Label" },
      { id: 3, label: "Other Label" }
    ];
    const result = validateConditionalText(existing, buildConditionalText());

    expect(result.hasErrors).toBe(true);
    expect(result.label).toBe("Label already exists.");
  });

  it("does not flag a duplicate when the only match is itself (same id)", () => {
    const existing = [{ id: 1, label: "My Label" }];
    const result = validateConditionalText(existing, buildConditionalText());

    expect(result.hasErrors).toBe(false);
    expect(result.label).toBeNull();
  });

  it("flags missing result content (no html)", () => {
    const result = validateConditionalText(
      [],
      buildConditionalText({ result: { html: "" } })
    );

    expect(result.hasErrors).toBe(true);
    expect(result.result).toBe("Content to insert is required.");
  });

  it("propagates rule set errors", () => {
    const result = validateConditionalText(
      [],
      buildConditionalText({
        ruleSets: [{ rules: [] }]
      })
    );

    expect(result.hasErrors).toBe(true);
    expect(result.ruleSets[0].message).toBe(
      "Rule set must have at least one rule."
    );
  });

  it("passes with valid, fully specified rule sets", () => {
    const result = validateConditionalText(
      [],
      buildConditionalText({
        ruleSets: [
          {
            rules: [
              {
                assignment: { lmsId: "a1" },
                comparison: { id: "eq", requiresValue: true },
                value: "42"
              }
            ]
          }
        ]
      })
    );

    expect(result.hasErrors).toBe(false);
    expect(result.ruleSets[0].hasRulesError).toBe(false);
  });
});

describe("validateMessage", () => {
  function buildMessage(overrides = {}) {
    return {
      configuration: {
        enabled: true,
        subject: "Hello",
        ...(overrides.configuration || {})
      },
      content: {
        html: "<p>Body</p>",
        ...(overrides.content || {})
      },
      ruleSets: overrides.ruleSets || []
    };
  }

  it("returns no errors and skips validation entirely when the message is not enabled", () => {
    const message = buildMessage({ configuration: { enabled: false, subject: "" } });
    const result = validateMessage(message, [], null);

    expect(result.hasErrors).toBe(false);
    expect(result.subject).toBeNull();
    expect(result.body).toBeNull();
  });

  it("passes validation for a fully valid, enabled message with no conditional text", () => {
    const message = buildMessage();
    const result = validateMessage(message, [], null);

    expect(result.hasErrors).toBe(false);
    expect(result.subject).toBeNull();
    expect(result.body).toBeNull();
  });

  it("flags a missing subject when enabled", () => {
    const message = buildMessage({ configuration: { enabled: true, subject: "" } });
    const result = validateMessage(message, [], null);

    expect(result.hasErrors).toBe(true);
    expect(result.subject).toBe("Subject line is required.");
  });

  it("flags a subject that is only whitespace", () => {
    const message = buildMessage({ configuration: { enabled: true, subject: "   " } });
    const result = validateMessage(message, [], null);

    expect(result.hasErrors).toBe(true);
    expect(result.subject).toBe("Subject line is required.");
  });

  it("flags a missing body when enabled", () => {
    const message = buildMessage({ content: { html: "" } });
    const result = validateMessage(message, [], null);

    expect(result.hasErrors).toBe(true);
    expect(result.body).toBe("Message body is required.");
  });

  it("flags recipient rule set errors", () => {
    const message = buildMessage({ ruleSets: [{ rules: [] }] });
    const result = validateMessage(message, [], null);

    expect(result.hasErrors).toBe(true);
    expect(result.recipients.hasErrors).toBe(true);
    expect(result.recipients.ruleSets[0].message).toBe(
      "Rule set must have at least one rule."
    );
  });

  it("validates the passed conditional text and rolls its errors up when provided", () => {
    const message = buildMessage();
    const conditionalText = {
      id: 1,
      label: "",
      result: { html: "content" },
      ruleSets: []
    };

    const result = validateMessage(message, [], conditionalText);

    expect(result.hasErrors).toBe(true);
    expect(result.conditionalText.hasErrors).toBe(true);
    expect(result.conditionalText.label).toBe("Label is required.");
  });

  it("does not evaluate conditional text errors when none is passed", () => {
    const message = buildMessage();
    const result = validateMessage(message, [], null);

    expect(result.conditionalText.hasErrors).toBe(false);
  });
});
