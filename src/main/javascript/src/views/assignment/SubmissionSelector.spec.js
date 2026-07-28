import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import SubmissionSelector from "./SubmissionSelector.vue";

const buildSubmissions = () => [
  { submissionId: 1, dateSubmitted: 100 },
  { submissionId: 2, dateSubmitted: 300 },
  { submissionId: 3, dateSubmitted: 200 }
];

describe("SubmissionSelector", () => {
  it("orders submissions newest-first and labels them by attempt number", () => {
    const wrapper = mountComponent(SubmissionSelector, {
      props: {
        submissions: buildSubmissions()
      }
    });

    const select = wrapper.findComponent({ name: "VSelect" });

    expect(select.props("items")).toEqual([
      { value: 2, label: "Attempt 3" },
      { value: 3, label: "Attempt 2" },
      { value: 1, label: "Attempt 1" }
    ]);
  });

  it("automatically selects the most recently submitted attempt on mount", () => {
    const wrapper = mountComponent(SubmissionSelector, {
      props: {
        submissions: buildSubmissions()
      }
    });

    expect(wrapper.findComponent({ name: "VSelect" }).props("modelValue")).toBe(2);
  });

  it("emits select whenever the active submission id changes", async () => {
    const wrapper = mountComponent(SubmissionSelector, {
      props: {
        submissions: buildSubmissions()
      }
    });

    // initial selection (from the immediate watcher) also fires the watcher
    expect(wrapper.emitted("select")).toBeTruthy();
    expect(wrapper.emitted("select").at(-1)).toEqual([2]);

    await wrapper.findComponent({ name: "VSelect" }).setValue(3);

    expect(wrapper.emitted("select").at(-1)).toEqual([3]);
  });

  it("re-selects the latest submission when the submissions prop changes", async () => {
    const wrapper = mountComponent(SubmissionSelector, {
      props: {
        submissions: buildSubmissions()
      }
    });

    await wrapper.setProps({
      submissions: [
        { submissionId: 4, dateSubmitted: 500 },
        ...buildSubmissions()
      ]
    });

    expect(wrapper.findComponent({ name: "VSelect" }).props("modelValue")).toBe(4);
    expect(wrapper.emitted("select").at(-1)).toEqual([4]);
  });

  it("falls back to no selection when there are no submissions", () => {
    const wrapper = mountComponent(SubmissionSelector, {
      props: {
        submissions: []
      }
    });

    expect(wrapper.findComponent({ name: "VSelect" }).props("modelValue")).toBe(null);
  });

});
