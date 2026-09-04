import { describe, expect, it, vi } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import PlaceholderList from "./PlaceholderList.vue";

const items = [
  { id: 1, type: "user", label: "First Name" },
  { id: 2, type: "user", label: "Last Name" },
  { id: 3, type: "course", label: "Course Name" }
];

describe("PlaceholderList", () => {
  it("renders a button per item when items are present", () => {
    const wrapper = mountComponent(PlaceholderList, {
      props: {
        items,
        command: vi.fn()
      }
    });

    const buttons = wrapper.findAll("button");

    expect(buttons).toHaveLength(3);
    expect(buttons[0].text()).toBe("First Name");
    expect(buttons[2].text()).toBe("Course Name");
  });

  it("renders 'No result' when items is empty", () => {
    const wrapper = mountComponent(PlaceholderList, {
      props: {
        items: [],
        command: vi.fn()
      }
    });

    expect(wrapper.find("button").exists()).toBe(false);
    expect(wrapper.find(".item").text()).toBe("No result");
  });

  it("marks the first item as selected by default", () => {
    const wrapper = mountComponent(PlaceholderList, {
      props: {
        items,
        command: vi.fn()
      }
    });

    const buttons = wrapper.findAll("button");

    expect(buttons[0].classes()).toContain("is-selected");
    expect(buttons[1].classes()).not.toContain("is-selected");
  });

  it("calls command with id and label when an item is clicked", async () => {
    const command = vi.fn();
    const wrapper = mountComponent(PlaceholderList, {
      props: {
        items,
        command
      }
    });

    await wrapper.findAll("button")[1].trigger("click");

    expect(command).toHaveBeenCalledWith({
      id: "user_2",
      label: "Last Name"
    });
  });

  it("resets selectedIndex to 0 when items change", async () => {
    const command = vi.fn();
    const wrapper = mountComponent(PlaceholderList, {
      props: {
        items,
        command
      }
    });

    await wrapper.findAll("button")[2].trigger("click");

    // move selection down via exposed onKeyDown, then change items and confirm reset
    wrapper.vm.onKeyDown({ event: { key: "ArrowDown" } });
    await wrapper.vm.$nextTick();

    expect(wrapper.findAll("button")[1].classes()).toContain("is-selected");

    await wrapper.setProps({ items: [items[0]] });

    expect(wrapper.findAll("button")[0].classes()).toContain("is-selected");
  });

  it("exposes onKeyDown that navigates with ArrowUp/ArrowDown and selects with Enter", async () => {
    const command = vi.fn();
    const wrapper = mountComponent(PlaceholderList, {
      props: {
        items,
        command
      }
    });

    // ArrowUp from index 0 wraps to the last item
    let handled = wrapper.vm.onKeyDown({ event: { key: "ArrowUp" } });
    await wrapper.vm.$nextTick();

    expect(handled).toBe(true);
    expect(wrapper.findAll("button")[2].classes()).toContain("is-selected");

    handled = wrapper.vm.onKeyDown({ event: { key: "Enter" } });

    expect(handled).toBe(true);
    expect(command).toHaveBeenCalledWith({
      id: "course_3",
      label: "Course Name"
    });
  });

  it("returns false from onKeyDown for unhandled keys", () => {
    const wrapper = mountComponent(PlaceholderList, {
      props: {
        items,
        command: vi.fn()
      }
    });

    expect(wrapper.vm.onKeyDown({ event: { key: "Tab" } })).toBe(false);
  });
});
