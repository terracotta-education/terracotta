import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import IntegrationsPreview from "./IntegrationsPreview.vue";

describe("IntegrationsPreview", () => {
  it("renders an iframe with the base64-decoded url as its src", () => {
    const decoded = "https://example.com/survey?launch_token=abc";
    const encoded = window.btoa(decoded);

    const wrapper = mountComponent(IntegrationsPreview, {
      props: { url: encoded }
    });

    const iframe = wrapper.find("iframe");
    expect(iframe.exists()).toBe(true);
    expect(iframe.attributes("src")).toBe(decoded);
  });

  it("re-decodes the src when the url prop changes", async () => {
    const first = window.btoa("https://example.com/first");
    const second = window.btoa("https://example.com/second");

    const wrapper = mountComponent(IntegrationsPreview, {
      props: { url: first }
    });

    await wrapper.setProps({ url: second });

    expect(wrapper.find("iframe").attributes("src")).toBe(
      "https://example.com/second"
    );
  });
});
