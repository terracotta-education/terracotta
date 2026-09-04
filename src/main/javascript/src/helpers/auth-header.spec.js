import { describe, it, expect, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { initHeader, authHeader, fileAuthHeader } from "./auth-header";
import { api } from "@/store/api.module";

describe("auth-header", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = api();
  });

  describe("initHeader", () => {
    it("returns an empty object when there is no LTI token", () => {
      expect(initHeader()).toEqual({});
    });

    it("returns an Authorization header built from the LTI token", () => {
      store.ltiToken = "my-lti-token";

      expect(initHeader()).toEqual({
        Authorization: "Bearer my-lti-token",
        "Content-Type": "application/json"
      });
    });
  });

  describe("authHeader", () => {
    it("returns an empty object when there is no API token", () => {
      expect(authHeader()).toEqual({});
    });

    it("returns an Authorization header built from the API token", () => {
      store.apiToken = "my-api-token";

      expect(authHeader()).toEqual({
        Authorization: "Bearer my-api-token",
        "Content-Type": "application/json"
      });
    });
  });

  describe("fileAuthHeader", () => {
    it("returns an empty object when there is no API token", () => {
      expect(fileAuthHeader()).toEqual({});
    });

    it("returns an Authorization header without a Content-Type when there is an API token", () => {
      store.apiToken = "my-api-token";

      expect(fileAuthHeader()).toEqual({
        Authorization: "Bearer my-api-token"
      });
      expect(fileAuthHeader()["Content-Type"]).toBeUndefined();
    });
  });
});
