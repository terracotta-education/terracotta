import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import CopyTreatmentMenu from "./CopyTreatmentMenu.vue";

const assignments = [
  { assignmentId: 1, title: "Pre-test" },
  { assignmentId: 2, title: "Post-test" }
];

describe("CopyTreatmentMenu", () => {
  it("does not render the menu activator when there are no assignments to copy from", () => {
    const wrapper = mountComponent(CopyTreatmentMenu, {
      props: { assignments: [] }
    });

    expect(wrapper.findComponent({ name: "VBtn" }).exists()).toBe(false);
  });

  it("renders the activator and a list item per assignment when opened", async () => {
    const wrapper = mountComponent(CopyTreatmentMenu, {
      props: { assignments }
    });

    expect(wrapper.text()).toContain("Copy Content From");

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    const items = wrapper.findAllComponents({ name: "VListItem" });
    expect(items).toHaveLength(2);
    expect(items[0].text()).toContain("Pre-test");
    expect(items[1].text()).toContain("Post-test");
  });

  it("emits duplicate with the selected assignment when a list item is clicked", async () => {
    const wrapper = mountComponent(CopyTreatmentMenu, {
      props: { assignments }
    });

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    const items = wrapper.findAllComponents({ name: "VListItem" });
    await items[1].trigger("click");

    expect(wrapper.emitted("duplicate")).toBeTruthy();
    expect(wrapper.emitted("duplicate")[0]).toEqual([assignments[1]]);
  });
});
