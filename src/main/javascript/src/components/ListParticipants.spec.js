import { describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ListParticipants from "./ListParticipants.vue";

const buildParticipants = () => [
  {
    participantId: "1",
    userId: "u1",
    user: { userId: "u1", displayName: "Alice" }
  },
  {
    participantId: "2",
    userId: "u2",
    user: { userId: "u2", displayName: "Bob" }
  }
];

describe("ListParticipants", () => {
  it("renders nothing when there are no participants", () => {
    const wrapper = mountComponent(ListParticipants, {
      props: {
        listOfParticipants: [],
        moveToOptions: ["Group A"],
        moveToHandler: vi.fn(),
        selectedOption: "0"
      }
    });

    expect(wrapper.find(".list-participants-container").exists()).toBe(false);
  });

  it("renders a list item with the display name for each participant", () => {
    const wrapper = mountComponent(ListParticipants, {
      props: {
        listOfParticipants: buildParticipants(),
        moveToOptions: ["Group A", "Group B"],
        moveToHandler: vi.fn(),
        selectedOption: "0"
      }
    });

    expect(wrapper.text()).toContain("Alice");
    expect(wrapper.text()).toContain("Bob");
    expect(wrapper.text()).toContain("0 Selected");
  });

  it("omits the currently selected option from the MOVE TO menu list", async () => {
    const wrapper = mountComponent(ListParticipants, {
      props: {
        listOfParticipants: buildParticipants(),
        moveToOptions: ["Group A", "Group B"],
        moveToHandler: vi.fn(),
        selectedOption: "0"
      }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");
    await wrapper.vm.$nextTick();

    const listItemTitles = wrapper
      .findAllComponents({ name: "VListItemTitle" })
      .map(c => c.text());

    expect(listItemTitles).toContain("Group B");
    expect(listItemTitles).not.toContain("Group A");
  });

  it("toggles a participant's selected state and updates the selected count when its row is clicked", async () => {
    const wrapper = mountComponent(ListParticipants, {
      props: {
        listOfParticipants: buildParticipants(),
        moveToOptions: ["Group A"],
        moveToHandler: vi.fn(),
        selectedOption: "0"
      }
    });

    const participantItems = wrapper.findAll(".participant-item");

    await participantItems[0].trigger("click");

    expect(wrapper.text()).toContain("1 Selected");

    await participantItems[0].trigger("click");

    expect(wrapper.text()).toContain("0 Selected");
  });

  it("selects all participants when the select-all checkbox is toggled on from empty", async () => {
    const wrapper = mountComponent(ListParticipants, {
      props: {
        listOfParticipants: buildParticipants(),
        moveToOptions: ["Group A"],
        moveToHandler: vi.fn(),
        selectedOption: "0"
      }
    });

    const checkbox = wrapper.findComponent({ name: "VCheckbox" });

    await checkbox.vm.$emit("update:model-value", true);
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).toContain("2 Selected");
  });

  it("calls moveToHandler with the selected participants and clears the selection", async () => {
    const moveToHandler = vi.fn();

    const wrapper = mountComponent(ListParticipants, {
      props: {
        listOfParticipants: buildParticipants(),
        moveToOptions: ["Group A"],
        moveToHandler,
        selectedOption: "1"
      }
    });

    const participants = buildParticipants();

    await wrapper.findAll(".participant-item")[0].trigger("click");
    expect(wrapper.text()).toContain("1 Selected");

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");
    await wrapper.vm.$nextTick();

    const menuItem = wrapper
      .findAllComponents({ name: "VListItemTitle" })
      .find(c => c.text() === "Group A");

    await menuItem.trigger("click");

    expect(moveToHandler).toHaveBeenCalledWith("Group A", [participants[0]]);
    expect(wrapper.text()).toContain("0 Selected");
  });
});
