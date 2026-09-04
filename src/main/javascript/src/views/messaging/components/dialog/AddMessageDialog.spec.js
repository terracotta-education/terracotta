import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import AddMessageDialog from "./AddMessageDialog.vue";

describe("AddMessageDialog", () => {
  let wrapper;

  afterEach(() => {
    wrapper?.unmount();
  });

  describe("single condition experiment", () => {
    it("renders a Create message button when there is no existing message", () => {
      wrapper = mountComponent(AddMessageDialog, {
        props: {
          hasExisting: false,
          isSingleConditionExperiment: true
        }
      });

      expect(wrapper.find(".btn-create-first-message").exists()).toBe(true);
      expect(wrapper.text()).toContain("Create message");
      expect(wrapper.findComponent({ name: "VMenu" }).exists()).toBe(false);
    });

    it("renders an Add message button when there is an existing message", () => {
      wrapper = mountComponent(AddMessageDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: true
        }
      });

      expect(wrapper.text()).toContain("Add message");
      expect(wrapper.find(".btn-create-first-message").exists()).toBe(false);
    });

    it("emits add with SINGLE when the button is clicked", async () => {
      wrapper = mountComponent(AddMessageDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: true
        }
      });

      await wrapper.findComponent({ name: "VBtn" }).trigger("click");

      expect(wrapper.emitted("add")).toEqual([["SINGLE"]]);
    });
  });

  describe("multi-condition experiment", () => {
    it("renders a Create Message activator behind a menu when there is no existing message", () => {
      wrapper = mountComponent(AddMessageDialog, {
        props: {
          hasExisting: false,
          isSingleConditionExperiment: false
        }
      });

      expect(wrapper.findComponent({ name: "VMenu" }).exists()).toBe(true);
      expect(wrapper.text()).toContain("Create Message");
    });

    it("renders an Add Message activator when there is an existing message", () => {
      wrapper = mountComponent(AddMessageDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: false
        }
      });

      expect(wrapper.text()).toContain("Add Message");
    });

    it("emits add with MULTIPLE and closes the menu when the different-versions option is chosen", async () => {
      wrapper = mountComponent(AddMessageDialog, {
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

      expect(wrapper.emitted("add")).toEqual([["MULTIPLE"]]);
      expect(wrapper.vm.addMessageDialogOpen).toBe(false);
    });

    it("emits add with SINGLE when the single-version option is chosen", async () => {
      wrapper = mountComponent(AddMessageDialog, {
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

      expect(wrapper.emitted("add")).toEqual([["SINGLE"]]);
    });

    it("disables the Add Message button while the menu is open", async () => {
      wrapper = mountComponent(AddMessageDialog, {
        props: {
          hasExisting: true,
          isSingleConditionExperiment: false
        }
      });

      const menu = wrapper.findComponent({ name: "VMenu" });
      await menu.setValue(true);
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.disableAddMessageButton).toBe(true);
    });
  });
});
