import { beforeEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { conditionaltext } from "./conditionaltext.module";

describe("conditionaltext store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = conditionaltext();
  });

  it("starts empty", () => {
    expect(store.messageConditionalTexts).toEqual([]);
    expect(store.messageConditionalText).toBeNull();
    expect(store.messageConditionalTextEditId).toBeNull();
    expect(store.hasConditionalTexts).toBe(false);
  });

  describe("addConditionalTexts", () => {
    it("adds new entries by label", () => {
      store.addConditionalTexts([
        { label: "a", value: 1 },
        { label: "b", value: 2 }
      ]);

      expect(store.messageConditionalTexts).toEqual([
        { label: "a", value: 1 },
        { label: "b", value: 2 }
      ]);
      expect(store.hasConditionalTexts).toBe(true);
    });

    it("replaces an existing entry with the same label", () => {
      store.addConditionalTexts([{ label: "a", value: 1 }]);
      store.addConditionalTexts([{ label: "a", value: 99 }]);

      expect(store.messageConditionalTexts).toEqual([
        { label: "a", value: 99 }
      ]);
    });

    it("skips falsy items in the array", () => {
      store.addConditionalTexts([null, { label: "a", value: 1 }, undefined]);

      expect(store.messageConditionalTexts).toEqual([
        { label: "a", value: 1 }
      ]);
    });

    it("does nothing when given a non-array", () => {
      store.addConditionalTexts("not-an-array");
      store.addConditionalTexts(null);
      store.addConditionalTexts(undefined);

      expect(store.messageConditionalTexts).toEqual([]);
    });

    it("addMessageConditionalTexts delegates to addConditionalTexts", () => {
      store.addMessageConditionalTexts([{ label: "a", value: 1 }]);

      expect(store.messageConditionalTexts).toEqual([
        { label: "a", value: 1 }
      ]);
    });
  });

  describe("setConditionalText / setMessageConditionalText", () => {
    it("sets the current conditional text", () => {
      store.setConditionalText({ label: "a" });
      expect(store.messageConditionalText).toEqual({ label: "a" });
    });

    it("setMessageConditionalText delegates to setConditionalText", () => {
      store.setMessageConditionalText({ label: "b" });
      expect(store.messageConditionalText).toEqual({ label: "b" });
    });
  });

  describe("setConditionalTextEditId / setMessageConditionalTextEditId", () => {
    it("sets the edit id", () => {
      store.setConditionalTextEditId(42);
      expect(store.messageConditionalTextEditId).toBe(42);
    });

    it("setMessageConditionalTextEditId delegates to setConditionalTextEditId", () => {
      store.setMessageConditionalTextEditId(7);
      expect(store.messageConditionalTextEditId).toBe(7);
    });
  });

  describe("reset", () => {
    it("clears all state", () => {
      store.addConditionalTexts([{ label: "a", value: 1 }]);
      store.setConditionalText({ label: "a" });
      store.setConditionalTextEditId(1);

      store.reset();

      expect(store.messageConditionalTexts).toEqual([]);
      expect(store.messageConditionalText).toBeNull();
      expect(store.messageConditionalTextEditId).toBeNull();
    });
  });
});
