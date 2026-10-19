import { afterEach, describe, expect, it, vi } from "vitest";

// Vue Router checks the route table when the router is created and warns about problems like a
// route repeating a param its parent already has, or an alias with different params than its
// route. Those warnings showed in the console on every page load.
describe("route table", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("creates the router without Vue Router warnings", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    vi.resetModules();

    await import("./index.js");

    const routerWarnings = warn.mock.calls.map(args => String(args[0])).filter(message => message.includes("VUE_ROUTER") || message.includes("[Vue Router"));
    expect(routerWarnings).toEqual([]);
  });
});
