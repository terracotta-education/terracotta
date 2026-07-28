import { afterEach, describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ComponentActionsMenu from "./ComponentActionsMenu.vue";
import { message as messageStatus } from "@/helpers/messaging/status.js";

// v-menu content is teleported to document.body (outside the wrapper's own
// element), so we query document.body directly and unmount after every test
// to avoid leaking teleported nodes into the next test.
let wrapper;

afterEach(() => {
  wrapper?.unmount();
  wrapper = undefined;
});

const assignmentRow = {
  type: "assignment",
  title: "Assignment 1",
  treatments: []
};

const messageRow = (status, treatmentStatuses = []) => ({
  type: "message",
  title: "Message 1",
  configuration: { status },
  treatments: treatmentStatuses.map(status => ({ status }))
});

const itemTitles = () =>
  Array.from(document.body.querySelectorAll(".v-list-item-title")).map(el => el.textContent.trim());

const clickItem = async label => {
  const item = Array.from(document.body.querySelectorAll(".v-list-item")).find(
    el => el.textContent.includes(label)
  );

  item.dispatchEvent(new MouseEvent("click", { bubbles: true }));
  await wrapper.vm.$nextTick();
};

const mountMenu = async props => {
  wrapper = mountComponent(ComponentActionsMenu, {
    props: {
      modelValue: true,
      row: assignmentRow,
      canDeleteAssignment: false,
      exposureCount: 1,
      hasIncompleteTreatments: vi.fn(() => false),
      ...props
    }
  });

  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("ComponentActionsMenu", () => {
  it("renders the actions activator button", async () => {
    await mountMenu();

    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(true);
  });

  it("always shows Edit and Duplicate for an assignment row", async () => {
    await mountMenu();
    const titles = itemTitles();

    expect(titles.some(text => text.includes("Edit"))).toBe(true);
    expect(titles.some(text => text.includes("Duplicate"))).toBe(true);
  });

  it("hides Move when exposureCount is 1 or less", async () => {
    await mountMenu({ exposureCount: 1 });

    expect(itemTitles().some(text => text.includes("Move"))).toBe(false);
  });

  it("shows Move for an assignment row when exposureCount > 1", async () => {
    await mountMenu({ exposureCount: 2 });

    expect(itemTitles().some(text => text.includes("Move"))).toBe(true);
  });

  it("shows Delete for an assignment row only when canDeleteAssignment is true", async () => {
    await mountMenu({ canDeleteAssignment: false });

    expect(itemTitles().some(text => text.includes("Delete"))).toBe(false);

    wrapper.unmount();

    await mountMenu({ canDeleteAssignment: true });

    expect(itemTitles().some(text => text.includes("Delete"))).toBe(true);
  });

  it("never shows Publish/Unpublish for an assignment row", async () => {
    await mountMenu({ canDeleteAssignment: true });
    const titles = itemTitles();

    expect(titles.some(text => text.includes("Publish"))).toBe(false);
    expect(titles.some(text => text.includes("Unpublish"))).toBe(false);
  });

  it("shows Publish for an unpublished message row with no incomplete treatments", async () => {
    await mountMenu({
      row: messageRow(messageStatus.unpublished),
      exposureCount: 2,
      hasIncompleteTreatments: vi.fn(() => false)
    });
    const titles = itemTitles();

    expect(titles.some(text => text.includes("Publish") && !text.includes("Unpublish"))).toBe(true);
  });

  it("hides Publish for an unpublished message row when treatments are incomplete", async () => {
    await mountMenu({
      row: messageRow(messageStatus.unpublished),
      hasIncompleteTreatments: vi.fn(() => true)
    });

    expect(itemTitles().some(text => text.includes("Publish") && !text.includes("Unpublish"))).toBe(false);
  });

  it("shows Unpublish for a published message row", async () => {
    await mountMenu({
      row: messageRow(messageStatus.published)
    });

    expect(itemTitles().some(text => text.includes("Unpublish"))).toBe(true);
  });

  it("hides Delete for a message row that has already been sent", async () => {
    await mountMenu({
      row: messageRow(messageStatus.sent)
    });

    expect(itemTitles().some(text => text.includes("Delete"))).toBe(false);
  });

  it("hides Delete for a message row where a treatment is sent", async () => {
    await mountMenu({
      row: messageRow(messageStatus.ready, [messageStatus.sent])
    });

    expect(itemTitles().some(text => text.includes("Delete"))).toBe(false);
  });

  it("shows Delete for a message row that is editable and has no sent treatments", async () => {
    await mountMenu({
      row: messageRow(messageStatus.ready, [messageStatus.ready])
    });

    expect(itemTitles().some(text => text.includes("Delete"))).toBe(true);
  });

  it("emits edit with the row when Edit is clicked", async () => {
    await mountMenu();

    await clickItem("Edit");

    expect(wrapper.emitted("edit")).toBeTruthy();
    expect(wrapper.emitted("edit")[0][0]).toEqual(assignmentRow);
  });

  it("emits duplicate with the row when Duplicate is clicked", async () => {
    await mountMenu();

    await clickItem("Duplicate");

    expect(wrapper.emitted("duplicate")).toBeTruthy();
    expect(wrapper.emitted("duplicate")[0][0]).toEqual(assignmentRow);
  });

  it("emits move with the row when Move is clicked", async () => {
    await mountMenu({ exposureCount: 2 });

    await clickItem("Move");

    expect(wrapper.emitted("move")).toBeTruthy();
    expect(wrapper.emitted("move")[0][0]).toEqual(assignmentRow);
  });

  it("emits delete with the row when Delete is clicked", async () => {
    await mountMenu({ canDeleteAssignment: true });

    await clickItem("Delete");

    expect(wrapper.emitted("delete")).toBeTruthy();
    expect(wrapper.emitted("delete")[0][0]).toEqual(assignmentRow);
  });

  it("emits update:modelValue when the menu open state changes", async () => {
    await mountMenu();

    await wrapper.setProps({ modelValue: false });

    expect(wrapper.props("modelValue")).toBe(false);
  });
});
