import { afterEach, describe, expect, it } from "vitest";
import { createPinia, setActivePinia } from "pinia";

import { mountComponent } from "@/test-utils/mount";
import { conditionaltext as useConditionalTextStore } from "@/store/messaging/conditionaltext.module";
import { message as useMessageStore } from "@/store/messaging/message.module";
import EditorSubMenu from "./EditorSubMenu.vue";

const baseProps = {
  experimentId: 1,
  exposureId: "exposure-1",
  containerId: "container-1",
  messageId: "message-1",
  contentId: "content-1"
};

const setupStores = ({
  assignments = [{ title: "Score", lmsId: "score", comparisons: [] }],
  pipedText = null,
  conditionalTexts = []
} = {}) => {
  const pinia = createPinia();
  setActivePinia(pinia);

  useMessageStore().assignments = assignments;
  useMessageStore().pipedText = pipedText;
  useConditionalTextStore().messageConditionalTexts = conditionalTexts;

  return pinia;
};

const stubs = { FileList: true };

// These menus use open-on-hover (rather than v-model) and teleport their
// content to document.body once opened, so they need a real hover event
// plus a short wait for Vuetify's activation delay, and must be inspected
// via the document rather than the wrapper's own subtree.
const openHoverMenu = async (wrapper, buttonText) => {
  const button = wrapper
    .findAllComponents({ name: "VBtn" })
    .find(b => b.text() === buttonText);

  await button.trigger("mouseenter");

  for (let i = 0; i < 10; i += 1) {
    await wrapper.vm.$nextTick();
    await new Promise(resolve => setTimeout(resolve, 50));

    if (document.querySelector(".v-overlay--active")) {
      break;
    }
  }
};

describe("EditorSubMenu", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
    document.body.innerHTML = "";
  });

  it("renders FileList when showAttachments is true", () => {
    const pinia = setupStores();

    wrapper = mountComponent(EditorSubMenu, {
      props: { ...baseProps, showAttachments: true },
      pinia,
      global: { stubs }
    });

    expect(wrapper.findComponent({ name: "FileList" }).exists()).toBe(true);
  });

  it("does not render FileList when showAttachments is false", () => {
    const pinia = setupStores();

    wrapper = mountComponent(EditorSubMenu, {
      props: { ...baseProps, showAttachments: false },
      pinia,
      global: { stubs }
    });

    expect(wrapper.findComponent({ name: "FileList" }).exists()).toBe(false);
  });

  it("hides the piped text menu when there are no piped text items", () => {
    const pinia = setupStores({ pipedText: { items: [] } });

    wrapper = mountComponent(EditorSubMenu, {
      props: baseProps,
      pinia,
      global: { stubs }
    });

    expect(wrapper.text()).not.toContain("INSERT PIPED TEXT");
  });

  it("shows the piped text menu and emits insertPipedText with the chosen item", async () => {
    const pipedItem = { id: "p1", key: "student_name" };
    const pinia = setupStores({ pipedText: { items: [pipedItem] } });

    wrapper = mountComponent(EditorSubMenu, {
      props: baseProps,
      pinia,
      global: { stubs }
    });

    expect(wrapper.text()).toContain("INSERT PIPED TEXT");

    await openHoverMenu(wrapper, "INSERT PIPED TEXT");

    const item = document.querySelector("[aria-label='select piped text']");
    expect(item).toBeTruthy();

    item.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("insertPipedText")).toEqual([[pipedItem]]);
  });

  it("shows a plain 'insert conditional text' button, disabled without rule assignments, when none exist yet", () => {
    const pinia = setupStores({ assignments: [], conditionalTexts: [] });

    wrapper = mountComponent(EditorSubMenu, {
      props: baseProps,
      pinia,
      global: { stubs }
    });

    expect(wrapper.text()).toContain("INSERT CONDITIONAL TEXT");
    expect(wrapper.findComponent({ name: "VMenu" }).exists()).toBe(false);

    const button = wrapper.findAllComponents({ name: "VBtn" }).find(
      b => b.text() === "INSERT CONDITIONAL TEXT"
    );
    expect(button.props("disabled")).toBe(true);
  });

  it("emits addConditionalText when the plain button is clicked with assignments available", async () => {
    const pinia = setupStores({ conditionalTexts: [] });

    wrapper = mountComponent(EditorSubMenu, {
      props: baseProps,
      pinia,
      global: { stubs }
    });

    const button = wrapper.findAllComponents({ name: "VBtn" }).find(
      b => b.text() === "INSERT CONDITIONAL TEXT"
    );

    expect(button.props("disabled")).toBe(false);
    await button.trigger("click");

    expect(wrapper.emitted("addConditionalText")).toBeTruthy();
  });

  it("shows a conditional text menu with truncated labels once conditional texts exist", async () => {
    const longLabel = "a".repeat(40);
    const pinia = setupStores({
      conditionalTexts: [{ id: "ct1", label: longLabel }]
    });

    wrapper = mountComponent(EditorSubMenu, {
      props: baseProps,
      pinia,
      global: { stubs }
    });

    expect(wrapper.findComponent({ name: "VMenu" }).exists()).toBe(true);

    await openHoverMenu(wrapper, "INSERT CONDITIONAL TEXT");

    expect(document.body.textContent).toContain(`${"a".repeat(29)}... `);
    expect(document.body.textContent).toContain("Add new conditional text");
  });

  it("emits insertConditionalText and editConditionalText from the menu items", async () => {
    const conditionalText = { id: "ct1", label: "Short label" };
    const pinia = setupStores({ conditionalTexts: [conditionalText] });

    wrapper = mountComponent(EditorSubMenu, {
      props: baseProps,
      pinia,
      global: { stubs }
    });

    await openHoverMenu(wrapper, "INSERT CONDITIONAL TEXT");

    document
      .querySelector(".conditional-text-item")
      .dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("insertConditionalText")).toEqual([[conditionalText]]);

    document
      .querySelector(".conditional-text-item-edit")
      .dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("editConditionalText")).toEqual([["ct1"]]);
  });

  it("hides insert controls entirely when readOnly is true", () => {
    const pinia = setupStores({
      pipedText: { items: [{ id: "p1", key: "name" }] },
      conditionalTexts: [{ id: "ct1", label: "Label" }]
    });

    wrapper = mountComponent(EditorSubMenu, {
      props: { ...baseProps, readOnly: true },
      pinia,
      global: { stubs }
    });

    expect(wrapper.text()).not.toContain("INSERT PIPED TEXT");
    expect(wrapper.text()).not.toContain("INSERT CONDITIONAL TEXT");
  });
});
