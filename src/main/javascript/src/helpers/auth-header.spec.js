import { describe, it, expect, beforeEach } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { initHeader, authHeader, fileAuthHeader } from "./auth-header";
import { api } from "@/store/api.module";

function base64url(obj) {
  return Buffer.from(JSON.stringify(obj))
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function makeToken(payload) {
  return `${base64url({ alg: "HS256", typ: "JWT" })}.${base64url(payload)}.signature`;
}

function validToken() {
  return makeToken({ exp: Math.floor(Date.now() / 1000) + 3600 });
}

function expiredToken() {
  return makeToken({ exp: Math.floor(Date.now() / 1000) - 3600 });
}

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
      store.apiToken = validToken();

      expect(authHeader()).toEqual({
        Authorization: `Bearer ${store.apiToken}`,
        "Content-Type": "application/json"
      });
    });

    // this is the one shared choke point every service builds its request headers through, so
    // it's what catches an expired token being used regardless of which page/component is
    // making the call - see the identical rationale on authHeader() itself.
    it("marks the session expired when the API token has already expired", () => {
      store.apiToken = expiredToken();

      authHeader();

      expect(store.sessionExpired).toBe(true);
    });

    it("does not mark the session expired when the API token is still valid", () => {
      store.apiToken = validToken();

      authHeader();

      expect(store.sessionExpired).toBe(false);
    });
  });

  describe("fileAuthHeader", () => {
    it("returns an empty object when there is no API token", () => {
      expect(fileAuthHeader()).toEqual({});
    });

    it("returns an Authorization header without a Content-Type when there is an API token", () => {
      store.apiToken = validToken();

      expect(fileAuthHeader()).toEqual({
        Authorization: `Bearer ${store.apiToken}`
      });
      expect(fileAuthHeader()["Content-Type"]).toBeUndefined();
    });

    it("marks the session expired when the API token has already expired", () => {
      store.apiToken = expiredToken();

      fileAuthHeader();

      expect(store.sessionExpired).toBe(true);
    });
  });
});
