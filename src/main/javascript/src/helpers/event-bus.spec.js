import { describe, it, expect, vi } from "vitest";

import { EventBus } from "./event-bus";

describe("EventBus", () => {
  it("delivers an emitted event to a registered listener", () => {
    const handler = vi.fn();

    EventBus.on("test-event", handler);
    EventBus.emit("test-event", { payload: 1 });

    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler).toHaveBeenCalledWith({ payload: 1 });

    EventBus.off("test-event", handler);
  });

  it("stops delivering events after a listener is removed", () => {
    const handler = vi.fn();

    EventBus.on("another-event", handler);
    EventBus.off("another-event", handler);
    EventBus.emit("another-event", {});

    expect(handler).not.toHaveBeenCalled();
  });

  it("supports multiple listeners for the same event", () => {
    const first = vi.fn();
    const second = vi.fn();

    EventBus.on("multi-event", first);
    EventBus.on("multi-event", second);
    EventBus.emit("multi-event", "payload");

    expect(first).toHaveBeenCalledWith("payload");
    expect(second).toHaveBeenCalledWith("payload");

    EventBus.off("multi-event", first);
    EventBus.off("multi-event", second);
  });
});
