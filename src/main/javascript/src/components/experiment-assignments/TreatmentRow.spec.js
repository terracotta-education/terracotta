import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import TreatmentRow from "./TreatmentRow.vue";
import { message as messageStatus } from "@/helpers/messaging/status.js";

// v-menu content is teleported to document.body (outside the wrapper's own
// element), so we open the menu with a real click and query document.body
// directly, matching ComponentActionsMenu.spec.js's convention.
let wrapper;

afterEach(() => {
  wrapper?.unmount();
  wrapper = undefined;
});

const exposure = {
  groupConditionList: [
    { conditionId: 1, conditionName: "Condition A" },
    { conditionId: 2, conditionName: "Condition B" }
  ]
};

const assignmentRow = (treatmentsCount = 2) => ({
  type: "assignment",
  assignmentId: 100,
  title: "Assignment 1",
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
  title: "Message 1",
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
      exposure,
      ...props
    }
  });

  return wrapper;
};

const activator = () => wrapper.find('[aria-label^="treatment actions for"]');

const openMenu = async () => {
  await activator().trigger("click");
};

const itemTitles = () =>
  Array.from(document.body.querySelectorAll(".v-list-item-title")).map(el => el.textContent.trim());

const findItem = label =>
  Array.from(document.body.querySelectorAll(".v-list-item")).find(el => el.textContent.includes(label));

const clickItem = async label => {
  findItem(label).dispatchEvent(new MouseEvent("click", { bubbles: true }));
  await wrapper.vm.$nextTick();
};

describe("TreatmentRow", () => {
  it("renders the wrench icon in an icon-circle for a complete assignment treatment", () => {
    mountRow({
      row: assignmentRow(),
      treatment: fileTreatment()
    });

    expect(wrapper.find(".component-icon").classes()).toContain("mdi-wrench-outline");
    expect(wrapper.find(".icon-circle").classes()).toContain("icon-circle-control");
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

  it("shows the integration icon in the code icon-circle for an assignment treatment with integration enabled", () => {
    mountRow({
      row: assignmentRow(),
      treatment: integrationTreatment()
    });

    expect(wrapper.find(".component-icon").classes()).toContain("mdi-application-brackets-outline");
    expect(wrapper.find(".icon-circle").classes()).toContain("icon-circle-code");
  });

  it("renders a single dots-menu activator with an aria-label naming the row", () => {
    mountRow({
      row: assignmentRow(),
      treatment: fileTreatment()
    });

    expect(activator().attributes("aria-label")).toBe("treatment actions for Assignment 1");
  });

  it("opens the menu with Edit and Preview for a plain assignment treatment, and Preview emits preview-treatment", async () => {
    const treatment = fileTreatment();

    mountRow({
      row: assignmentRow(),
      treatment
    });

    await openMenu();
    const titles = itemTitles();

    expect(titles.some(text => text.includes("Edit"))).toBe(true);
    expect(titles.some(text => text.includes("Preview"))).toBe(true);

    await clickItem("Preview");

    expect(wrapper.emitted("preview-treatment")).toBeTruthy();
    expect(wrapper.emitted("preview-treatment")[0][0]).toEqual(treatment);
  });

  it("emits edit-treatment with row and treatment when Edit is clicked", async () => {
    const row = assignmentRow();
    const treatment = fileTreatment();

    mountRow({ row, treatment });

    await openMenu();
    await clickItem("Edit");

    expect(wrapper.emitted("edit-treatment")).toBeTruthy();
    expect(wrapper.emitted("edit-treatment")[0][0]).toEqual({ row, treatment });
  });

  it("shows an enabled Preview integration link when the integration URL is valid", async () => {
    mountRow({
      row: assignmentRow(),
      treatment: integrationTreatment()
    });

    await openMenu();

    const previewItem = findItem("Preview");
    expect(previewItem.classList.contains("v-list-item--disabled")).toBe(false);
    expect(document.body.querySelector(".integration-preview-link")).toBeTruthy();
  });

  it("disables the Preview item, but keeps Edit clickable, when the integration URL is invalid", async () => {
    const row = assignmentRow();
    const treatment = integrationTreatment({ integrationUrlValid: false });

    mountRow({ row, treatment });

    await openMenu();

    expect(findItem("Preview").classList.contains("v-list-item--disabled")).toBe(true);

    await clickItem("Edit");

    expect(wrapper.emitted("edit-treatment")).toBeTruthy();
    expect(wrapper.emitted("edit-treatment")[0][0]).toEqual({ row, treatment });
  });

  it("shows the condition name as the row's label for a complete treatment", () => {
    mountRow({
      row: assignmentRow(2),
      treatment: fileTreatment()
    });

    expect(wrapper.find(".treatment-condition-name").text()).toBe("Condition A");
  });

  it("shows 'Treatment' instead of the condition name for a single-version (Only One Version) row", () => {
    mountRow({
      row: assignmentRow(1),
      treatment: fileTreatment()
    });

    expect(wrapper.find(".treatment-condition-name").text()).toBe("Treatment");
  });

  it("falls back to 'No condition name' when a condition has no name (should never happen, but isn't silently blank if it does)", () => {
    mountRow({
      row: assignmentRow(2),
      treatment: fileTreatment(),
      exposure: {
        groupConditionList: [{ conditionId: 1, conditionName: "" }, exposure.groupConditionList[1]]
      }
    });

    expect(wrapper.find(".treatment-condition-name").text()).toBe("No condition name");
  });

  it("shows the message icon in the message icon-circle and status-driven label for a message treatment", () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.ready)
    });

    expect(wrapper.find(".component-icon").classes()).toContain("mdi-message-text-outline");
    expect(wrapper.find(".icon-circle").classes()).toContain("icon-circle-message");
    expect(wrapper.find(".label-treatment-complete").exists()).toBe(true);
  });

  it("shows Edit for a not-yet-sent message treatment and View for a sent one", async () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.ready)
    });
    await openMenu();
    expect(itemTitles().some(text => text.includes("Edit"))).toBe(true);
    wrapper.unmount();

    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.sent)
    });
    await openMenu();
    expect(itemTitles().some(text => text.includes("View"))).toBe(true);
  });

  it("marks an incomplete message treatment status (e.g. incomplete) with the incomplete label and tooltip", () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.incomplete)
    });

    expect(wrapper.find(".label-treatment-incomplete").exists()).toBe(true);
    expect(wrapper.findComponent({ name: "ToolTip" }).exists()).toBe(true);
  });

  it("hides Preview entirely (only Edit/View) for a message treatment", async () => {
    mountRow({
      row: messageTreatmentRow(),
      treatment: messageTreatment(messageStatus.ready)
    });

    await openMenu();

    expect(itemTitles().some(text => text.includes("Preview"))).toBe(false);
  });
});
