import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import LinkDialog from "./LinkDialog.vue";

// VDialog teleports its content to document.body, so once open its card
// markup lives outside the wrapper's own element tree. We query the
// document directly for that content and unmount after every test so
// teleported nodes from one test don't leak into the next.
describe("LinkDialog", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  const setUrlValue = async value => {
    const input = document.querySelector(".input-url input");

    input.value = value;
    input.dispatchEvent(new Event("input"));

    await wrapper.vm.$nextTick();
  };

  const findButtonByText = text => {
    const buttons = Array.from(document.querySelectorAll("button"));

    return buttons.find(button => button.textContent.trim() === text);
  };

  it("renders with the dialog open by default", () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {}
      }
    });

    expect(wrapper.findComponent({ name: "VDialog" }).exists()).toBe(true);
    expect(document.querySelector(".input-url")).not.toBeNull();
  });

  it("pre-fills the URL field from the href prop", async () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {},
        href: "https://example.com"
      }
    });

    await wrapper.vm.$nextTick();

    expect(document.querySelector(".input-url input").value).toBe(
      "https://example.com"
    );
  });

  it("disables the Apply button when the URL is empty", () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {}
      }
    });

    const applyButton = findButtonByText("APPLY");

    expect(applyButton.disabled).toBe(true);
  });

  it("enables the Apply button once a URL is entered and emits submit with the URL", async () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {}
      }
    });

    await setUrlValue("https://example.com");

    const applyButton = findButtonByText("APPLY");

    expect(applyButton.disabled).toBe(false);

    applyButton.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("submit")).toBeTruthy();
    expect(wrapper.emitted("submit")[0]).toEqual(["https://example.com"]);
  });

  it("closes the dialog after applying", async () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {}
      }
    });

    await setUrlValue("https://example.com");

    const applyButton = findButtonByText("APPLY");

    applyButton.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.dialog).toBe(false);
  });

  it("emits close when the close icon button is clicked", async () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {}
      }
    });

    const closeIconButton = Array.from(
      document.querySelectorAll("button")
    ).find(button => button.querySelector(".mdi-close"));

    expect(closeIconButton).not.toBeNull();

    closeIconButton.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("close")).toBeTruthy();
  });

  it("emits close when the CLOSE text button is clicked", async () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {}
      }
    });

    const closeButton = findButtonByText("CLOSE");

    closeButton.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("close")).toBeTruthy();
  });

  it("updates the URL field when the href prop changes", async () => {
    wrapper = mountComponent(LinkDialog, {
      props: {
        editor: {},
        href: ""
      }
    });

    await wrapper.setProps({ href: "https://updated.example.com" });

    expect(document.querySelector(".input-url input").value).toBe(
      "https://updated.example.com"
    );
  });
});
