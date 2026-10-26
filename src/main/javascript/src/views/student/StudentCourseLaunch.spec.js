import { beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";

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

  it("tells the student where their Terracotta assignments are in an alert", () => {
    swalFire.mockReturnValue(new Promise(() => {}));
    mountComponent(StudentCourseLaunch);

    expect(swalFire).toHaveBeenCalledWith(expect.objectContaining({ icon: "info", text: MESSAGE, target: "#app" }));
  });

  it("keeps the page clear while the alert is open", async () => {
    swalFire.mockReturnValue(new Promise(() => {}));
    const wrapper = mountComponent(StudentCourseLaunch);
    await flushPromises();

    expect(wrapper.text()).not.toContain(MESSAGE);
  });

  it("shows the message on the page once the alert is closed, instead of a blank page", async () => {
    let close;
    swalFire.mockReturnValue(new Promise(resolve => { close = resolve; }));
    const wrapper = mountComponent(StudentCourseLaunch);

    close({ isConfirmed: true });
    await flushPromises();

    expect(wrapper.text()).toContain(MESSAGE);
  });

  it("shows the message on the page when the alert can't be shown", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    swalFire.mockRejectedValue(new Error("no #app"));
    const wrapper = mountComponent(StudentCourseLaunch);
    await flushPromises();

    expect(wrapper.text()).toContain(MESSAGE);
  });

  it("reports itself loaded so the loading message goes away", () => {
    swalFire.mockReturnValue(new Promise(() => {}));
    const wrapper = mountComponent(StudentCourseLaunch);

    expect(wrapper.emitted("loaded")).toHaveLength(1);
  });
});
