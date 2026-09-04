import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import AddAssignmentDialog from "./AddAssignmentDialog.vue";

describe("AddAssignmentDialog", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  describe("single condition experiment", () => {
    it("renders a Create Assignment button when there is no existing assignment", () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: false,
          isSingleConditionExperiment: true
        }
      });

      expect(wrapper.find(".btn-create-first-assignment").exists()).toBe(true);
      expect(wrapper.text()).toContain("Create Assignment");
      expect(wrapper.findComponent({ name: "VMenu" }).exists()).toBe(false);
    });

    it("renders an Add Assignment button when there is an existing assignment", () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: true
        }
      });

      expect(wrapper.text()).toContain("Add Assignment");
      expect(wrapper.find(".btn-create-first-assignment").exists()).toBe(false);
    });

    it("emits single when the button is clicked", async () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: true
        }
      });

      await wrapper.findComponent({ name: "VBtn" }).trigger("click");

      expect(wrapper.emitted("single")).toBeTruthy();
    });
  });

  describe("multi-condition experiment", () => {
    it("renders a Create Assignment activator behind a menu when there is no existing assignment", () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: false,
          isSingleConditionExperiment: false
        }
      });

      expect(wrapper.findComponent({ name: "VMenu" }).exists()).toBe(true);
      expect(wrapper.text()).toContain("Create Assignment");
    });

    it("renders an Add Assignment activator when there is an existing assignment", () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: false
        }
      });

      expect(wrapper.text()).toContain("Add Assignment");
    });

    it("emits multiple and closes the menu when the different-versions option is chosen", async () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: false
        }
      });

      const menu = wrapper.findComponent({ name: "VMenu" });
      await menu.setValue(true);
      await wrapper.vm.$nextTick();

      const buttons = document.querySelectorAll("button[role='menuitem']");
      expect(buttons.length).toBe(2);

      buttons[0].dispatchEvent(new MouseEvent("click", { bubbles: true }));
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("multiple")).toBeTruthy();
      expect(wrapper.vm.addAssignmentDialogOpen).toBe(false);
    });

    it("emits single when the single-version option is chosen", async () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: false
        }
      });

      const menu = wrapper.findComponent({ name: "VMenu" });
      await menu.setValue(true);
      await wrapper.vm.$nextTick();

      const buttons = document.querySelectorAll("button[role='menuitem']");
      buttons[1].dispatchEvent(new MouseEvent("click", { bubbles: true }));
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("single")).toBeTruthy();
    });

    it("disables the Add Assignment button while the menu is open", async () => {
      wrapper = mountComponent(AddAssignmentDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: false
        }
      });

      const menu = wrapper.findComponent({ name: "VMenu" });
      await menu.setValue(true);
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.disableAddAssignmentButton).toBe(true);
    });
  });
});
