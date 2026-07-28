import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import { api } from "@/store/api.module";
import { configuration } from "@/store/configuration.module";
import OAuth2Redirect from "./OAuth2Redirect.vue";

describe("OAuth2Redirect", () => {
  it("falls back to a generic LMS title and no href when nothing is loaded yet", () => {
    const wrapper = mountComponent(OAuth2Redirect);

    expect(wrapper.text()).toContain("Terracotta wants to access your LMS account");
    expect(
      wrapper.find(".redirect-button").attributes("href")
    ).toBeFalsy();
  });

  it("renders the configured LMS title throughout the page", () => {
    const wrapper = mountComponent(OAuth2Redirect);

    const configurationStore = configuration();
    configurationStore.$patch({ configurations: { lmsTitle: "Canvas" } });

    return wrapper.vm.$nextTick().then(() => {
      expect(wrapper.text()).toContain("Terracotta wants to access your Canvas account");
      expect(wrapper.text()).toContain("Go to the Canvas Authorization Page");
    });
  });

  it("links the authorization button to the API store's OAuth URL", async () => {
    const wrapper = mountComponent(OAuth2Redirect);

    const apiStore = api();
    apiStore.$patch({ lmsApiOAuthURL: "https://lms.example.com/oauth/authorize" });

    await wrapper.vm.$nextTick();

    expect(wrapper.find(".redirect-button").attributes("href")).toBe(
      "https://lms.example.com/oauth/authorize"
    );
  });

  it("renders a tooltip explaining each requested permission", () => {
    const wrapper = mountComponent(OAuth2Redirect);

    const tooltips = wrapper.findAllComponents({ name: "ToolTip" });

    expect(tooltips).toHaveLength(2);
    expect(wrapper.text()).toContain("List assignment submissions");
    expect(wrapper.text()).toContain(
      "Create, list, edit and delete assignments"
    );
  });
});
