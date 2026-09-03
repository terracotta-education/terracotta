import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import TreatmentRow from "./TreatmentRow.vue";
import { message as messageStatus } from "@/helpers/messaging/status.js";

let wrapper;

afterEach(() => {
  wrapper?.unmount();
  wrapper = undefined;
});

const conditionColorMapping = {
  "Condition A": "blue",
  "Condition B": "red"
};

const exposure = {
  groupConditionList: [
    { conditionId: 1, conditionName: "Condition A" },
    { conditionId: 2, conditionName: "Condition B" }
  ]
};

const assignmentRow = (treatmentsCount = 2) => ({
  type: "assignment",
  assignmentId: 100,
  treatments: new Array(treatmentsCount).fill(null)
});

const fileTreatment = overrides => ({
  treatmentId: 10,
  conditionId: 1,
  assessmentDto: {
    integration: false,
    questions: [{ id: 1 }],
    ...overrides
  }
});

const integrationTreatment = overrides => ({
  treatmentId: 11,
  conditionId: 1,
  assessmentDto: {
    integration: true,
    integrationUrlValid: true,
    integrationPreviewUrl: "http://example.com/preview",
    questions: [{ id: 1 }],
    ...overrides
  }
});

const messageTreatmentRow = () => ({
  type: "message",
  assignmentId: 200,
  treatments: [null, null]
});

const messageTreatment = status => ({
  treatmentId: 20,
  conditionId: 2,
  configuration: { status }
});

const mountRow = props => {
  wrapper = mountComponent(TreatmentRow, {
    props: {
      conditions: exposure.groupConditionList,
      conditionColorMapping,
      exposure,
      singleConditionExperiment: false,
      displayTreatmentMenu: false,
      ...props
    }
  });

  return wrapper;
};

describe("TreatmentRow", () => {
  it("renders the file icon and Edit button for a complete assignment treatment", () => {
    mountRow({
      row: assignmentRow(),
      treatment: fileTreatment()
    });

    expect(wrapper.find(".component-icon").classes()).toContain("mdi-wrench-outline");
    expect(wrapper.find(".btn-treatment-edit").text()).toContain("Edit");
    expect(wrapper.find(".label-treatment-complete").exists()).toBe(true);
  });

  it("shows the incomplete tooltip and label for an assignment treatment with no questions", () => {
    mountRow({
      row: assignmentRow(),
      treatment: fileTreatment({ questions: [] })
    });

    expect(wrapper.find(".label-treatment-incomplete").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(true);
  });

  it("shows the integration icon for an assignment treatment with integration enabled", () => {
    mountRow({
      row: assignmentRow(),
      treatment: integrationTreatment()
    });

    expect(wrapper.find(".component-icon").classes()).toContain("mdi-application-brackets-outline");
  });

  it("shows a disabled Preview link for an integration treatment when not using the treatment menu", () => {
    mountRow({
      row: assignmentRow(),
      treatment: integrationTreatment(),
      displayTreatmentMenu: false
    });

    const links = wrapper.findAllComponents({ name: "VBtn" });
    const preview = links.find(btn => btn.text().includes("Preview"));

    expect(preview.exists()).toBe(true);
    expect(preview.props("disabled")).toBe(false);
  });

  it("disables the Preview link when the integration URL is invalid", () => {
    mountRow({
      row: assignmentRow(),
      treatment: integrationTreatment({ integrationUrlValid: false }),
      displayTreatmentMenu: false
    });

    const links = wrapper.findAllComponents({ name: "VBtn" });
    const preview = links.find(btn => btn.text().includes("Preview"));

    expect(preview.props("disabled")).toBe(true);
  });

  it("shows a Preview button (not menu) for a non-integration, non-message treatment and emits preview-treatment", async () => {
    const treatment = fileTreatment();

    mountRow({
      row: assignmentRow(),
      treatment
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });
    const preview = buttons.find(btn => btn.text().includes("Preview"));

    await preview.trigger("click");

    expect(wrapper.emitted("preview-treatment")).toBeTruthy();
    expect(wrapper.emitted("preview-treatment")[0][0]).toEqual(treatment);
  });

  it("emits edit-treatment with row and treatment when Edit is clicked", async () => {
    const row = assignmentRow();
    const treatment = fileTreatment();

    mountRow({ row, treatment });

    await wrapper.find(".btn-treatment-edit").trigger("click");

    expect(wrapper.emitted("edit-treatment")).toBeTruthy();
    expect(wrapper.emitted("edit-treatment")[0][0]).toEqual({ row, treatment });
  });

  it("shows a condition chip only when treatments count matches conditions count and not single-condition", () => {
    mountRow({
      row: assignmentRow(2),
      treatment: fileTreatment()
    });

    const chip = wrapper.findComponent({ name: "VChip" });

    expect(chip.exists()).toBe(true);
    expect(chip.text()).toBe("Condition A");
  });

  it("hides the condition chip for a single condition experiment", () => {
    mountRow({
      row: assignmentRow(2),
      treatment: fileTreatment(),
      singleConditionExperiment: true
    });

    expect(wrapper.findComponent({ name: "VChip" }).exists()).toBe(false);
  });

  it("hides the condition chip when treatment count does not match condition count", () => {
    mountRow({
      row: assignmentRow(1),
      treatment: fileTreatment()
    });

    expect(wrapper.findComponent({ name: "VChip" }).exists()).toBe(false);
  });

  it("renders a dots menu with a Preview link for an integration treatment when displayTreatmentMenu is true", () => {
    mountRow({
      row: assignmentRow(),
      treatment: integrationTreatment(),
      displayTreatmentMenu: true
    });

    expect(wrapper.find('[aria-label="treatment actions"]').exists()).toBe(true);
    // the plain Preview v-btn should not be rendered when the menu variant is used
    const buttons = wrapper.findAllComponents({ name: "VBtn" });

    expect(buttons.some(btn => btn.text().includes("Preview"))).toBe(false);
  });

  it("disables the dots menu activator when the underlying preview is disabled, matching the non-menu button", () => {
    mountRow({
      row: assignmentRow(),
      treatment: integrationTreatment({ integrationUrlValid: false }),
      displayTreatmentMenu: true
    });

    const activator = wrapper.find('[aria-label="treatment actions"]');

    expect(activator.attributes("disabled")).toBeDefined();
  });

  it("shows the message icon and status-driven label for a message treatment", () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.ready)
    });

    expect(wrapper.find(".component-icon").classes()).toContain("mdi-message-text-outline");
    expect(wrapper.find(".label-treatment-complete").exists()).toBe(true);
    expect(wrapper.find(".btn-treatment-edit").text()).toContain("Edit");
  });

  it("shows View instead of Edit for a message treatment that has already been sent", () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.sent)
    });

    expect(wrapper.find(".btn-treatment-edit").text()).toContain("View");
    expect(wrapper.find(".label-treatment-complete").exists()).toBe(true);
  });

  it("marks an incomplete message treatment status (e.g. incomplete) with the incomplete label and tooltip", () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.incomplete)
    });

    expect(wrapper.find(".label-treatment-incomplete").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(true);
  });

  it("does not show a plain Preview button for a message treatment", () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.ready)
    });

    const buttons = wrapper.findAllComponents({ name: "VBtn" });

    expect(buttons.some(btn => btn.text().includes("Preview"))).toBe(false);
  });
});
