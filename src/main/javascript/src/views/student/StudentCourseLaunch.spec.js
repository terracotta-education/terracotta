import { beforeEach, describe, expect, it, vi } from "vitest";

const { swalFire } = vi.hoisted(() => ({ swalFire: vi.fn() }));

vi.mock("sweetalert2", () => ({
  default: { fire: (...args) => swalFire(...args) }
}));

import { mountComponent } from "@/test-utils/mount";
import StudentCourseLaunch from "./StudentCourseLaunch.vue";

const MESSAGE = "Terracotta assignments are just like other assignments. You will find them where you find the rest of your class assignments, in Assignments and/or Modules.";

describe("StudentCourseLaunch", () => {
  beforeEach(() => {
    swalFire.mockReset();
  });

  it("tells the student where their Terracotta assignments are, in an alert and on the page", () => {
    const wrapper = mountComponent(StudentCourseLaunch);

    expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({ icon: "info", text: MESSAGE, target: "#app" }));
    // still there after the alert is closed, instead of a blank page
    expect(wrapper.text()).toContain(MESSAGE);
  });

  it("reports itself loaded so the loading message goes away", () => {
    const wrapper = mountComponent(StudentCourseLaunch);

    expect(wrapper.emitted("loaded")).toHaveLength(1);
  });
});
