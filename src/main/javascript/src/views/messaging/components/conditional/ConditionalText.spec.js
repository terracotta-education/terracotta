import { describe, expect, it, vi, beforeEach } from "vitest";

vi.mock("sweetalert2", () => ({
  default: {
    fire: vi.fn()
  }
}));

import { createPinia, setActivePinia } from "pinia";
import Swal from "sweetalert2";

import { mountComponent } from "@/test-utils/mount";
import ConditionalText from "./ConditionalText.vue";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";
import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";

const TipTapEditorStub = {
  name: "TipTapEditor",
  props: ["content", "editorType", "readOnly", "pipedTextToPlace", "required"],
  emits: ["edited", "cursor"],
  template:
    "<div class=\"tiptap-stub\" @click=\"$emit('edited', '<p>Some result content</p>')\" />"
};

const EditorSubMenuStub = {
  name: "EditorSubMenu",
  props: [
    "experimentId",
    "exposureId",
    "containerId",
    "messageId",
    "contentId",
    "showAttachments",
    "showConditionalText"
  ],
  template: "<div class=\"editor-sub-menu-stub\" />"
};

const baseProps = {
  experimentId: 1,
  exposureId: "exposure-1",
  containerId: "container-1",
  messageId: "message-1",
  contentId: "content-1",
  maxRuleCount: 2
};

function buildAssignment(overrides = {}) {
  return {
    lmsId: "assignment-1",
    title: "Assignment 1",
    comparisons: [
      { id: "eq", label: "Equals", requiresValue: true },
      { id: "submitted", label: "Submitted", requiresValue: false }
    ],
    ...overrides
  };
}

function seedStores({
  status = "READY",
  editId = null,
  assignments = [buildAssignment()],
  existingConditionalTexts = []
} = {}) {
  const pinia = createPinia();
  setActivePinia(pinia);

  const containerStore = messagingMessageContainerModule();
  containerStore.messageContainers = [
    {
      id: "container-1",
      messages: [
        {
          id: "message-1",
          configuration: { status },
          content: { conditionalTexts: [] }
        }
      ]
    }
  ];

  const messageStore = messagingMessageModule();
  messageStore.assignments = assignments;

  const conditionalTextStore = messagingConditionalTextModule();
  conditionalTextStore.messageConditionalTexts = existingConditionalTexts;
  conditionalTextStore.messageConditionalTextEditId = editId;

  return { pinia, containerStore, messageStore, conditionalTextStore };
}

function mount(overrides = {}, seedOptions = {}) {
  const { pinia, conditionalTextStore } = seedStores(seedOptions);

  const wrapper = mountComponent(ConditionalText, {
    pinia,
    props: { ...baseProps, ...overrides },
    global: {
      stubs: {
        TipTapEditor: TipTapEditorStub,
        EditorSubMenu: EditorSubMenuStub
      }
    }
  });

  return { wrapper, conditionalTextStore };
}

describe("ConditionalText", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows the Insert heading and starts with one empty rule set when creating a new conditional text", () => {
    const { wrapper } = mount();

    expect(wrapper.text()).toContain("Insert Conditional Text");
    expect(wrapper.findAll(".rule-sets")).toHaveLength(1);
  });

  it("shows the Update heading when editing an existing conditional text", () => {
    const existing = {
      id: "ct-1",
      label: "My Condition",
      result: { html: "<p>Existing</p>" },
      ruleSets: [
        {
          id: "rs-1",
          operator: "NONE",
          rules: [
            {
              id: "r-1",
              operator: "NONE",
              lmsAssignmentId: "assignment-1",
              assignment: buildAssignment(),
              comparison: { id: "eq", label: "Equals", requiresValue: true },
              value: "5"
            }
          ]
        }
      ]
    };

    const { wrapper } = mount({}, { editId: "ct-1", existingConditionalTexts: [existing] });

    expect(wrapper.text()).toContain("Update Conditional Text");
    expect(
      wrapper.findComponent({ name: "VTextField" }).props("modelValue")
    ).toBe("My Condition");
  });

  it("adds a new rule set when 'Add a new set of rules' is clicked", async () => {
    const { wrapper } = mount();

    const addSetButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Add a new set of rules"));

    await addSetButton.trigger("click");

    expect(wrapper.findAll(".rule-sets")).toHaveLength(2);
  });

  it("disables adding more rules once maxRuleCount is reached", async () => {
    const { wrapper } = mount({ maxRuleCount: 1 });

    expect(wrapper.text()).toContain(
      "You have used the maximum number of rules (1)"
    );

    const addSetButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("Add a new set of rules"));

    expect(addSetButton.props("disabled")).toBe(true);
  });

  it("selecting a rule variable populates the comparison options and updates the rule", async () => {
    const { wrapper } = mount();

    const variableSelect = wrapper.findComponent({ name: "VSelect" });
    await variableSelect.setValue(buildAssignment());
    await wrapper.vm.$nextTick();

    const comparisonSelect = wrapper.findAllComponents({ name: "VSelect" })[1];
    expect(comparisonSelect.props("items")).toHaveLength(2);
  });

  it("clears the rule when Clear is clicked", async () => {
    const existing = {
      id: "ct-1",
      label: "My Condition",
      result: { html: "<p>Existing</p>" },
      ruleSets: [
        {
          id: "rs-1",
          operator: "NONE",
          rules: [
            {
              id: "r-1",
              operator: "NONE",
              lmsAssignmentId: "assignment-1",
              assignment: buildAssignment(),
              comparison: { id: "eq", label: "Equals", requiresValue: true },
              value: "5"
            }
          ]
        }
      ]
    };

    const { wrapper } = mount({}, { editId: "ct-1", existingConditionalTexts: [existing] });

    const clearButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text() === "Clear");

    await clearButton.trigger("click");
    await wrapper.vm.$nextTick();

    const valueField = wrapper.findComponent({ name: "VTextField" });
    // the label field is the only VTextField remaining after clearing the rule's value binding
    expect(valueField.props("modelValue")).toBe("My Condition");
  });

  it("shows a validation alert and does not emit when saving without a label", async () => {
    const { wrapper } = mount();

    const saveButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("SAVE & INSERT"));

    await saveButton.trigger("click");

    expect(Swal.fire).toHaveBeenCalledWith(
      "Please complete all required sections."
    );
    expect(wrapper.emitted("conditionalTextCreated")).toBeFalsy();
  });

  it("emits conditionalTextCreated with a complete, valid conditional text", async () => {
    const { wrapper, conditionalTextStore } = mount();

    await wrapper.findComponent({ name: "VTextField" }).setValue("My Label");

    const variableSelect = wrapper.findComponent({ name: "VSelect" });
    await variableSelect.setValue(buildAssignment());
    await wrapper.vm.$nextTick();

    const comparisonSelect = wrapper.findAllComponents({ name: "VSelect" })[1];
    await comparisonSelect.setValue({
      id: "submitted",
      label: "Submitted",
      requiresValue: false
    });
    await wrapper.vm.$nextTick();

    await wrapper.find(".tiptap-stub").trigger("click");
    await wrapper.vm.$nextTick();

    const saveButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text().includes("SAVE & INSERT"));

    await saveButton.trigger("click");

    expect(Swal.fire).not.toHaveBeenCalled();
    expect(wrapper.emitted("conditionalTextCreated")).toBeTruthy();

    const emittedConditionalText = wrapper.emitted("conditionalTextCreated")[0][0];
    expect(emittedConditionalText.label).toBe("My Label");
    expect(conditionalTextStore.messageConditionalTexts).toContainEqual(
      expect.objectContaining({ label: "My Label" })
    );
  });

  it("emits cancel and clears the store's edit state when Cancel is clicked", async () => {
    const { wrapper, conditionalTextStore } = mount();

    const cancelButton = wrapper
      .findAllComponents({ name: "VBtn" })
      .find(btn => btn.text() === "CANCEL");

    await cancelButton.trigger("click");

    expect(wrapper.emitted("cancel")).toBeTruthy();
    expect(conditionalTextStore.messageConditionalTextEditId).toBeNull();
  });

  it("is read-only and shows CLOSE instead of CANCEL when the message has been sent", () => {
    const { wrapper } = mount({}, { status: "SENT" });

    const buttons = wrapper.findAllComponents({ name: "VBtn" }).map(btn => btn.text());
    expect(buttons).toContain("CLOSE");
    expect(buttons).not.toContain("CANCEL");
    expect(buttons.some(text => text.includes("SAVE & INSERT"))).toBe(false);
  });
});
