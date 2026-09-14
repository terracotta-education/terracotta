import { afterEach, describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ComponentTable from "./ComponentTable.vue";
import { message as messageStatus } from "@/helpers/messaging/status.js";

let wrapper;

afterEach(() => {
  wrapper?.unmount();
  wrapper = undefined;
});

const exposure = {
  exposureId: 1,
  groupConditionList: [
    { conditionId: 1, conditionName: "Condition A" },
    { conditionId: 2, conditionName: "Condition B" },
    { conditionId: 3, conditionName: "Condition C" }
  ]
};

// deliberately a SEPARATE array/shape from exposure.groupConditionList above - the
// real `conditions` prop comes from experimentStore.conditions, whose items use
// "name" (see ComponentTable.vue's conditionDisplayName comment), not "conditionName"
// like the exposure DTO's groupConditionList. Using the same array for both here
// would hide exactly the field-name bug this fixture split now catches.
const conditions = [
  { conditionId: 1, name: "Condition A" },
  { conditionId: 2, name: "Condition B" },
  { conditionId: 3, name: "Condition C" }
];

const conditionColorMapping = {
  "Condition A": "blue",
  "Condition B": "red"
};

const completeTreatment = (id, conditionId = 1) => ({
  treatmentId: id,
  conditionId,
  assessmentDto: {
    integration: false,
    integrationUrlValid: true,
    questions: [{ id: 1 }]
  }
});

const incompleteTreatment = (id, conditionId = 1) => ({
  treatmentId: id,
  conditionId,
  assessmentDto: {
    integration: false,
    integrationUrlValid: true,
    questions: []
  }
});

const assignmentRow = overrides => ({
  type: "assignment",
  assignmentId: 1,
  title: "Assignment 1",
  assignmentOrder: 1,
  published: true,
  dueDate: "2024-05-01T12:00:00Z",
  treatments: [completeTreatment(10, 1), completeTreatment(11, 2), completeTreatment(12, 3)],
  ...overrides
});

const messageRow = overrides => ({
  type: "message",
  assignmentId: 2,
  title: "Message 1",
  assignmentOrder: 2,
  published: false,
  sent: false,
  error: false,
  dueDate: null,
  configuration: { status: messageStatus.ready },
  treatments: [
    { treatmentId: 20, conditionId: 1, configuration: { status: messageStatus.ready } },
    { treatmentId: 21, conditionId: 2, configuration: { status: messageStatus.ready } },
    { treatmentId: 22, conditionId: 3, configuration: { status: messageStatus.ready } }
  ],
  ...overrides
});

const mountTable = (rows, props = {}) => {
  wrapper = mountComponent(ComponentTable, {
    props: {
      rows,
      exposure,
      conditions,
      conditionColorMapping,
      singleConditionExperiment: false,
      canDeleteAssignment: true,
      exposureCount: 2,
      ...props
    }
  });

  return wrapper;
};

describe("ComponentTable", () => {
  it("renders one row per assignment with its title", () => {
    mountTable([assignmentRow(), messageRow()]);

    expect(wrapper.text()).toContain("Assignment 1");
    expect(wrapper.text()).toContain("Message 1");
  });

  it("shows the 'Only One Version' chip when a row has exactly one treatment", () => {
    mountTable([
      assignmentRow({ treatments: [completeTreatment(10)] })
    ]);

    expect(wrapper.find(".only-one-version-chip").exists()).toBe(true);
    expect(wrapper.text()).toContain("Only One Version");
  });

  it("does not show the 'Only One Version' chip when there is more than one treatment", () => {
    mountTable([assignmentRow()]);

    expect(wrapper.find(".only-one-version-chip").exists()).toBe(false);
  });

  it("shows the treatments count as complete/complete when all treatments are filled in", () => {
    mountTable([assignmentRow()]);

    expect(wrapper.find(".label-treatment-complete").exists()).toBe(true);
    expect(wrapper.text()).toContain("3 of 3");
  });

  it("shows the real treatments ratio (not a no-op) when a genuinely multi-version row has fewer treatments than conditions", () => {
    mountTable([
      assignmentRow({ treatments: [completeTreatment(10, 1), completeTreatment(11, 2)] })
    ]);

    expect(wrapper.text()).toContain("2 of 3");
    expect(wrapper.find(".label-treatment-incomplete").exists()).toBe(true);
  });

  it("marks the treatments column incomplete and shows a small-dot tooltip when a treatment is missing content", () => {
    mountTable([
      assignmentRow({ treatments: [completeTreatment(10, 1), completeTreatment(11, 2), incompleteTreatment(12, 3)] })
    ]);

    expect(wrapper.find(".label-treatment-incomplete").exists()).toBe(true);

    const tooltip = wrapper.findComponent({ name: "ToolTip" });
    expect(tooltip.exists()).toBe(true);
    expect(tooltip.props("icon")).toBe("mdi-circle");
  });

  it("counts a treatment record that exists but lacks content as NOT built yet, in both the ratio and the section label", () => {
    mountTable([
      assignmentRow({ treatments: [completeTreatment(10, 1), completeTreatment(11, 2), incompleteTreatment(12, 3)] })
    ]);

    // 3 treatment records exist, but only 2 have real content - the ratio and label
    // should both read "2 of 3", not "3 of 3"
    expect(wrapper.text()).toContain("2 of 3");
    expect(wrapper.text()).toContain("TREATMENTS - 2 of 3 built");
  });

  it("renders the 'TREATMENTS - X of Y built' section label for a multi-condition row", () => {
    mountTable([assignmentRow()]);
    expect(wrapper.text()).toContain("TREATMENTS - 3 of 3 built");
  });

  it("suppresses the 'TREATMENTS - X of Y built' section label for a single-condition experiment", () => {
    mountTable([assignmentRow()], { singleConditionExperiment: true });
    expect(wrapper.find(".treatments-section-label").exists()).toBe(false);
  });

  it("renders an add-treatment placeholder for each condition missing a treatment, and clicking it emits add-treatment", async () => {
    mountTable([
      assignmentRow({ treatments: [completeTreatment(10, 1), completeTreatment(11, 2)] })
    ]);

    const addBox = wrapper.find(".treatment-add-box");
    expect(addBox.exists()).toBe(true);
    expect(wrapper.text()).toContain("Click to build treatment");
    expect(wrapper.text()).toContain("Needs attention");

    await addBox.trigger("click");

    expect(wrapper.emitted("add-treatment")).toBeTruthy();
    expect(wrapper.emitted("add-treatment")[0][0]).toMatchObject({
      condition: { conditionId: 3, name: "Condition C" }
    });
  });

  it("renders no add-treatment placeholder when every condition already has a treatment", () => {
    mountTable([assignmentRow()]);

    expect(wrapper.find(".treatment-add-box").exists()).toBe(false);
  });

  it("falls back to 'No condition name' for the add-treatment placeholder when a condition has no name (should never happen, but isn't silently blank if it does)", () => {
    mountTable(
      [assignmentRow({ treatments: [completeTreatment(10, 1), completeTreatment(11, 2)] })],
      { conditions: [...conditions.slice(0, 2), { conditionId: 3, name: "" }] }
    );

    expect(wrapper.text()).toContain("No condition name");
  });

  it("also emits add-treatment when the 'Click to build treatment' link itself is clicked, and shows an actions menu with Edit enabled and Preview disabled", async () => {
    mountTable([
      assignmentRow({ treatments: [completeTreatment(10, 1), completeTreatment(11, 2)] })
    ]);

    const link = wrapper.find(".treatment-add-link");
    expect(link.exists()).toBe(true);

    await link.trigger("click");

    expect(wrapper.emitted("add-treatment")).toBeTruthy();

    const menuActivator = wrapper.find('[aria-label="treatment actions for Condition C"]');
    expect(menuActivator.exists()).toBe(true);

    await menuActivator.trigger("click");

    const editItem = Array.from(document.body.querySelectorAll(".v-list-item")).find(
      el => el.textContent.includes("Edit")
    );
    const previewItem = Array.from(document.body.querySelectorAll(".v-list-item")).find(
      el => el.textContent.includes("Preview")
    );

    expect(editItem.classList.contains("v-list-item--disabled")).toBe(false);
    expect(previewItem.classList.contains("v-list-item--disabled")).toBe(true);

    editItem.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("add-treatment")).toHaveLength(2);
  });

  // there's no persisted signal distinguishing "deliberately single-version" from
  // "multi-version but still incomplete" (both are just treatments.length === 1) -
  // matching the pre-remodel app's behavior, a row showing "Only One Version" is
  // always treated as complete against its own treatment count, not total conditions
  it("treats an 'Only One Version' row as complete against its own count, not total conditions - no placeholder, no incomplete dot", () => {
    mountTable([
      assignmentRow({ treatments: [completeTreatment(10, 1)] })
    ]);

    expect(wrapper.text()).toContain("Only One Version");
    expect(wrapper.text()).toContain("1 of 1");
    expect(wrapper.find(".treatment-add-box").exists()).toBe(false);
    expect(wrapper.find(".label-treatment-incomplete").exists()).toBe(false);
  });

  it("shows the add-treatment placeholder for an 'Only One Version' row's own treatment when it has no content, and clicking it emits edit-treatment (not add-treatment)", async () => {
    const row = assignmentRow({ treatments: [incompleteTreatment(10, 1)] });

    mountTable([row]);

    expect(wrapper.text()).toContain("Only One Version");
    // the ratio's numerator counts complete treatments, not just treatment records -
    // this one exists but has no content yet, so it doesn't count as "built"
    expect(wrapper.text()).toContain("0 of 1");
    expect(wrapper.find(".label-treatment-incomplete").exists()).toBe(true);

    const addBox = wrapper.find(".treatment-add-box");
    expect(addBox.exists()).toBe(true);

    await addBox.trigger("click");

    expect(wrapper.emitted("edit-treatment")).toBeTruthy();
    expect(wrapper.emitted("edit-treatment")[0][0]).toMatchObject({
      treatment: { treatmentId: 10 }
    });
    expect(wrapper.emitted("add-treatment")).toBeFalsy();
  });

  it("shows the add-treatment placeholder for an existing-but-incomplete treatment on a multi-version assignment, and clicking it emits edit-treatment", async () => {
    mountTable([
      assignmentRow({
        treatments: [completeTreatment(10, 1), completeTreatment(11, 2), incompleteTreatment(12, 3)]
      })
    ]);

    const addBoxes = wrapper.findAll(".treatment-add-box");
    expect(addBoxes).toHaveLength(1);

    await addBoxes[0].trigger("click");

    expect(wrapper.emitted("edit-treatment")).toBeTruthy();
    expect(wrapper.emitted("edit-treatment")[0][0]).toMatchObject({
      treatment: { treatmentId: 12, conditionId: 3 }
    });
    expect(wrapper.emitted("add-treatment")).toBeFalsy();
  });

  it("shows the code icon-circle for an incomplete integration treatment's placeholder", () => {
    const incompleteIntegrationTreatment = (id, conditionId) => ({
      treatmentId: id,
      conditionId,
      assessmentDto: { integration: true, integrationUrlValid: true, questions: [] }
    });

    mountTable([
      assignmentRow({
        treatments: [
          completeTreatment(10, 1),
          completeTreatment(11, 2),
          incompleteIntegrationTreatment(12, 3)
        ]
      })
    ]);

    const placeholderIcon = wrapper.find(".treatment-add-row .icon-circle");
    expect(placeholderIcon.classes()).toContain("icon-circle-code");
  });

  it("shows the add-treatment placeholder for an existing-but-incomplete message treatment, and clicking it emits edit-treatment", async () => {
    mountTable([
      messageRow({
        treatments: [
          { treatmentId: 20, conditionId: 1, configuration: { status: messageStatus.ready } },
          { treatmentId: 21, conditionId: 2, configuration: { status: messageStatus.ready } },
          { treatmentId: 22, conditionId: 3, configuration: { status: messageStatus.incomplete } }
        ]
      })
    ]);

    const addBoxes = wrapper.findAll(".treatment-add-box");
    expect(addBoxes).toHaveLength(1);

    await addBoxes[0].trigger("click");

    expect(wrapper.emitted("edit-treatment")).toBeTruthy();
    expect(wrapper.emitted("edit-treatment")[0][0]).toMatchObject({
      treatment: { treatmentId: 22, conditionId: 3 }
    });
    expect(wrapper.emitted("add-treatment")).toBeFalsy();
  });

  it("shows the status pill with the theme color matching each row's status", () => {
    mountTable([
      assignmentRow({ published: true }),
      assignmentRow({ assignmentId: 2, published: false }),
      messageRow({ assignmentId: 3, sent: true, published: false }),
      messageRow({ assignmentId: 4, error: true })
    ]);

    const pills = wrapper.findAllComponents({ name: "VChip" })
      .filter(chip => ["Published", "Unpublished", "Sent", "Error"].includes(chip.text()));

    expect(pills.find(chip => chip.text() === "Published").props("color")).toBe("success");
    expect(pills.find(chip => chip.text() === "Unpublished").props("color")).toBe("warning");
    expect(pills.find(chip => chip.text() === "Sent").props("color")).toBe("info");
    expect(pills.find(chip => chip.text() === "Error").props("color")).toBe("error");
  });

  it("renders the shortened, all-caps column headers", () => {
    mountTable([assignmentRow()]);

    expect(wrapper.text()).toContain("NAME");
    expect(wrapper.text()).toContain("DUE");
    expect(wrapper.text()).toContain("TREATMENTS");
    expect(wrapper.text()).toContain("STATUS");
  });

  it("formats the due date for assignment rows and leaves it blank when absent", () => {
    mountTable([
      assignmentRow({ dueDate: "2024-05-01T12:00:00Z" }),
      messageRow({ assignmentId: 3, dueDate: null })
    ]);

    expect(wrapper.text()).toMatch(/May 1, 2024/);
  });

  it("shows Published status for a published assignment and Unpublished otherwise", () => {
    mountTable([
      assignmentRow({ published: true }),
      messageRow({ assignmentId: 3 })
    ]);

    expect(wrapper.text()).toContain("Published");
    expect(wrapper.text()).toContain("Unpublished");
  });

  it("shows Sent status for a sent message row", () => {
    mountTable([
      messageRow({ sent: true, published: false })
    ]);

    expect(wrapper.text()).toContain("Sent");
  });

  it("shows Error status for a message row with an error", () => {
    mountTable([
      messageRow({ error: true })
    ]);

    expect(wrapper.text()).toContain("Error");
  });

  // VTooltip's content is teleported to document.body (like v-menu's, see this
  // file's own convention above) rather than staying under wrapper's own root, so
  // wrapper.text() alone won't see it - it's rendered there statically regardless of
  // open state, so no hover interaction is needed to assert it's wired up correctly.
  it("wires up hover-help tooltips explaining the Only One Version chip, due date, and Published/Unpublished/Needs attention pills", () => {
    mountTable([
      assignmentRow({ published: true, dueDate: "2024-05-01T12:00:00Z", treatments: [completeTreatment(10)] }),
      assignmentRow({ assignmentId: 2, published: false, treatments: [] })
    ]);

    const text = document.body.textContent;

    expect(text).toContain("This component has the same content for all students.");
    expect(text).toContain("A due date has been set for this component in the LMS.");
    expect(text).toContain("This component has been published in the LMS.");
    expect(text).toContain("This component has not yet been published in the LMS, and cannot be accessed by students.");
    expect(text).toContain("There are versions of this component that have not yet been created. Be sure to create all versions before publishing.");
  });

  it("skips the status tooltip for Sent/Error message rows, which have no reviewed copy", () => {
    mountTable([
      messageRow({ sent: true, published: false }),
      messageRow({ assignmentId: 3, error: true })
    ]);

    const text = document.body.textContent;

    expect(text).not.toContain("This component has been published in the LMS.");
    expect(text).not.toContain("This component has not yet been published in the LMS, and cannot be accessed by students.");
  });

  it("renders an actions menu button per row, with a row-specific aria-label", () => {
    mountTable([assignmentRow(), messageRow()]);

    const actionButtons = wrapper.findAll(".component-actions-btn");

    expect(actionButtons.length).toBe(2);
    actionButtons.forEach(btn => {
      expect(btn.attributes("aria-label")).toMatch(/^actions for .+/);
    });
  });

  it("expands all rows by default (expandedRows initialized from rows)", () => {
    mountTable([assignmentRow(), messageRow()]);

    // When expanded, treatment rows render inside expanded-row slot content.
    expect(wrapper.findAllComponents({ name: "TreatmentRow" }).length).toBeGreaterThan(0);
  });

  it("wraps the expanded-row slot content in its own <tr> (required by Vuetify 3's expanded-row slot contract, unlike Vuetify 2's auto-wrapping expanded-item slot)", () => {
    mountTable([assignmentRow()]);

    const expandedTr = wrapper.find("tr.v-data-table__tr--expanded");

    expect(expandedTr.exists()).toBe(true);
    expect(expandedTr.find("td.treatments-table-container").exists()).toBe(true);
  });

  it("re-initializes expandedRows when the rows prop changes", async () => {
    mountTable([assignmentRow()]);

    expect(wrapper.findAllComponents({ name: "TreatmentRow" }).length).toBe(3);

    await wrapper.setProps({ rows: [assignmentRow(), messageRow()] });

    expect(wrapper.text()).toContain("Message 1");
  });

  it("emits edit-treatment when a TreatmentRow requests an edit", async () => {
    mountTable([assignmentRow()]);

    const treatmentRow = wrapper.findComponent({ name: "TreatmentRow" });

    await treatmentRow.vm.$emit("edit-treatment", { row: assignmentRow(), treatment: completeTreatment(10) });

    expect(wrapper.emitted("edit-treatment")).toBeTruthy();
  });

  it("emits preview-treatment when a TreatmentRow requests a preview", async () => {
    mountTable([assignmentRow()]);

    const treatmentRow = wrapper.findComponent({ name: "TreatmentRow" });

    await treatmentRow.vm.$emit("preview-treatment", completeTreatment(10));

    expect(wrapper.emitted("preview-treatment")).toBeTruthy();
  });

  it("emits save-order when the table root receives a sorted custom event", async () => {
    mountTable([assignmentRow(), messageRow()]);

    const detail = { some: "sortable-event-detail" };

    wrapper.element.dispatchEvent(new CustomEvent("sorted", { detail, bubbles: true }));
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted("save-order")).toBeTruthy();
    expect(wrapper.emitted("save-order")[0][0]).toBe(detail);
  });

  it("labels each drag handle with the row's title and position, and moving it with arrow keys emits save-order", async () => {
    mountTable([assignmentRow(), messageRow()]);

    const handles = wrapper.findAll(".dragger");
    expect(handles).toHaveLength(2);
    expect(handles[0].attributes("aria-label")).toContain("Assignment 1");
    expect(handles[0].attributes("aria-label")).toContain("Position 1 of 2");

    await handles[0].trigger("keydown", { key: "ArrowDown" });

    expect(wrapper.emitted("save-order")).toBeTruthy();
    expect(wrapper.emitted("save-order")[0][0]).toEqual({
      oldDraggableIndex: 0,
      newDraggableIndex: 1,
      focusAssignmentId: 1
    });
  });

  it("does not emit save-order when arrowing past either end of the list", async () => {
    mountTable([assignmentRow(), messageRow()]);

    const handles = wrapper.findAll(".dragger");

    await handles[0].trigger("keydown", { key: "ArrowUp" });
    await handles[1].trigger("keydown", { key: "ArrowDown" });

    expect(wrapper.emitted("save-order")).toBeFalsy();
  });

  it("ignores keys other than the arrow keys on the drag handle", async () => {
    mountTable([assignmentRow(), messageRow()]);

    const handles = wrapper.findAll(".dragger");

    await handles[0].trigger("keydown", { key: "Enter" });

    expect(wrapper.emitted("save-order")).toBeFalsy();
  });

  // regression guard for a gap that appeared above "TREATMENTS - N of N built" on
  // the last component: a CSS rule keyed off tbody's structural :last-child <tr>
  // (meant to vertically-center the last assignment row's own content) actually
  // matched that row's own expanded-row sibling instead, since every row starts
  // expanded by default - assignment-row--last exists so that rule can target the
  // right element regardless of expand state. See ComponentTable.vue's row-props
  // and its .assignment-row--last CSS comment for the full explanation.
  it("marks only the last assignment row with assignment-row--last", () => {
    mountTable([assignmentRow(), messageRow()]);

    const rows = wrapper.findAll("tr.assignment-row");
    expect(rows).toHaveLength(2);
    expect(rows[0].classes()).not.toContain("assignment-row--last");
    expect(rows[1].classes()).toContain("assignment-row--last");
  });

  // regression guard for a missing group divider when a component is collapsed:
  // the bold group-to-group divider only ever came from a rule keyed off
  // .v-data-table__tr--expanded (see ComponentTable.vue's own comment on
  // .assignment-row--collapsed), which doesn't exist at all for a collapsed row,
  // so the divider between two collapsed components silently fell back to
  // Vuetify's own thin default border instead.
  it("marks a row assignment-row--collapsed only while it is actually collapsed", async () => {
    mountTable([assignmentRow(), messageRow()]);

    let rows = wrapper.findAll("tr.assignment-row");
    expect(rows[0].classes()).not.toContain("assignment-row--collapsed");

    const toggle = wrapper.find('[aria-label^="Expand component row"]');
    await toggle.trigger("click");

    rows = wrapper.findAll("tr.assignment-row");
    expect(rows[0].classes()).toContain("assignment-row--collapsed");

    await toggle.trigger("click");

    rows = wrapper.findAll("tr.assignment-row");
    expect(rows[0].classes()).not.toContain("assignment-row--collapsed");
  });

  it("renders no rows when given an empty rows array", () => {
    mountTable([]);

    expect(wrapper.findAllComponents({ name: "TreatmentRow" })).toHaveLength(0);
  });
});
