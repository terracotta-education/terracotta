import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import QuestionTypeMenu from "./QuestionTypeMenu.vue";

describe("QuestionTypeMenu", () => {
  it("renders the ADD QUESTION activator and all three question type options", async () => {
    const wrapper = mountComponent(QuestionTypeMenu);

    expect(wrapper.text()).toContain("ADD QUESTION");

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    const items = wrapper.findAllComponents({ name: "VListItem" });
    expect(items).toHaveLength(3);
    expect(items[0].text()).toContain("Short answer");
    expect(items[1].text()).toContain("Multiple choice");
    expect(items[2].text()).toContain("File submission");
  });

  it.each([
    [0, "ESSAY"],
    [1, "MC"],
    [2, "FILE"]
  ])("emits add-question with %s when option %i is clicked", async (index, type) => {
    const wrapper = mountComponent(QuestionTypeMenu);

    await wrapper.findComponent({ name: "VBtn" }).trigger("click");

    const items = wrapper.findAllComponents({ name: "VListItem" });
    await items[index].trigger("click");

    expect(wrapper.emitted("add-question")).toBeTruthy();
    expect(wrapper.emitted("add-question")[0]).toEqual([type]);
  });
});
