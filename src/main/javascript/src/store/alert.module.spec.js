import { beforeEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { alert } from "./alert.module";

describe("alert store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = alert();
  });

  it("starts with no alert", () => {
    expect(store.hasAlert).toBe(false);
  });

  it("info sets type and message", () => {
    store.info("hello");

    expect(store.alertType).toBe("info");
    expect(store.alertMessage).toBe("hello");
    expect(store.hasAlert).toBe(true);
  });

  it("clear resets state", () => {
    store.error("oops");
    store.clear();

    expect(store.hasAlert).toBe(false);
    expect(store.alertType).toBeNull();
    expect(store.alertMessage).toBeNull();
  });
});
