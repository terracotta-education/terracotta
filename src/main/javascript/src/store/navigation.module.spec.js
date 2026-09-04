import { beforeEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { navigation } from "./navigation.module";

describe("navigation store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = navigation();
  });

  it("starts with no edit mode", () => {
    expect(store.hasEditMode).toBe(false);
    expect(store.initialPage).toBeNull();
    expect(store.callerPage).toBeNull();
  });

  it("saveEditMode stores the edit mode object", () => {
    store.saveEditMode({ initialPage: "page1", callerPage: "page2" });

    expect(store.hasEditMode).toBe(true);
    expect(store.initialPage).toBe("page1");
    expect(store.callerPage).toBe("page2");
  });

  it("saveEditMode with falsy value clears edit mode", () => {
    store.saveEditMode({ initialPage: "page1", callerPage: "page2" });
    store.saveEditMode(null);

    expect(store.hasEditMode).toBe(false);
    expect(store.editMode).toBeNull();
  });

  it("saveEditMode with undefined clears edit mode", () => {
    store.saveEditMode({ initialPage: "page1", callerPage: "page2" });
    store.saveEditMode(undefined);

    expect(store.editMode).toBeNull();
  });

  it("initialPage / callerPage return null when editMode fields are missing", () => {
    store.saveEditMode({});

    // hasEditMode is just Boolean(editMode), so a non-null empty object is still "true"
    expect(store.hasEditMode).toBe(true);
    expect(store.initialPage).toBeNull();
    expect(store.callerPage).toBeNull();
  });

  it("deleteEditMode resets edit mode", () => {
    store.saveEditMode({ initialPage: "page1", callerPage: "page2" });
    store.deleteEditMode();

    expect(store.hasEditMode).toBe(false);
    expect(store.editMode).toBeNull();
  });

  it("resetNavigation resets edit mode", () => {
    store.saveEditMode({ initialPage: "page1", callerPage: "page2" });
    store.resetNavigation();

    expect(store.hasEditMode).toBe(false);
    expect(store.editMode).toBeNull();
  });
});
