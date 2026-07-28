import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import ExposureDesignCard from "./ExposureDesignCard.vue";

const conditionColorMapping = {
  "Condition A": "blue",
  "Condition B": "red",
  "Condition C": "green",
  "Condition D": "purple"
};

const buildExposure = groupConditionList => ({ groupConditionList });

describe("ExposureDesignCard", () => {
  it("renders each group with its mapped condition and chip color", () => {
    const wrapper = mountComponent(ExposureDesignCard, {
      props: {
        exposure: buildExposure([
          { groupName: "Group 1", conditionName: "Condition A", conditionId: 1 },
          { groupName: "Group 2", conditionName: "Condition B", conditionId: 2 }
        ]),
        conditionColorMapping
      }
    });

    const groupRows = wrapper.findAll(".groupNames");

    expect(groupRows).toHaveLength(2);
    expect(groupRows[0].text()).toContain("Group 1");
    expect(groupRows[0].text()).toContain("Condition A");
    expect(groupRows[1].text()).toContain("Group 2");
    expect(groupRows[1].text()).toContain("Condition B");
  });

  it("does not show the More/Less toggle when groups are within maxDesignGroups", () => {
    const wrapper = mountComponent(ExposureDesignCard, {
      props: {
        exposure: buildExposure([
          { groupName: "Group 1", conditionName: "Condition A", conditionId: 1 }
        ]),
        conditionColorMapping,
        maxDesignGroups: 2
      }
    });

    expect(wrapper.find("a.text-blue").exists()).toBe(false);
  });

  it("truncates to maxDesignGroups and shows a More toggle when there are extra groups", () => {
    const wrapper = mountComponent(ExposureDesignCard, {
      props: {
        exposure: buildExposure([
          { groupName: "Group C", conditionName: "Condition C", conditionId: 3 },
          { groupName: "Group A", conditionName: "Condition A", conditionId: 1 },
          { groupName: "Group B", conditionName: "Condition B", conditionId: 2 }
        ]),
        conditionColorMapping,
        maxDesignGroups: 2
      }
    });

    // groups are alphabetically sorted before slicing
    const groupRows = wrapper.findAll(".groupNames");

    expect(groupRows).toHaveLength(2);
    expect(groupRows[0].text()).toContain("Group A");
    expect(groupRows[1].text()).toContain("Group B");

    const toggle = wrapper.find("a.text-blue");

    expect(toggle.exists()).toBe(true);
    expect(toggle.text()).toContain("More");
  });

  it("expands to show all groups and toggles to Less when clicked", async () => {
    const wrapper = mountComponent(ExposureDesignCard, {
      props: {
        exposure: buildExposure([
          { groupName: "Group C", conditionName: "Condition C", conditionId: 3 },
          { groupName: "Group A", conditionName: "Condition A", conditionId: 1 },
          { groupName: "Group B", conditionName: "Condition B", conditionId: 2 }
        ]),
        conditionColorMapping,
        maxDesignGroups: 2
      }
    });

    await wrapper.find("a.text-blue").trigger("click");

    const groupRows = wrapper.findAll(".groupNames");

    expect(groupRows).toHaveLength(3);
    expect(wrapper.find("a.text-blue").text()).toContain("Less");

    await wrapper.find("a.text-blue").trigger("click");

    expect(wrapper.findAll(".groupNames")).toHaveLength(2);
    expect(wrapper.find("a.text-blue").text()).toContain("More");
  });

  it("handles an exposure with no groupConditionList gracefully", () => {
    const wrapper = mountComponent(ExposureDesignCard, {
      props: {
        exposure: {},
        conditionColorMapping
      }
    });

    expect(wrapper.findAll(".groupNames")).toHaveLength(0);
    expect(wrapper.find("a.text-blue").exists()).toBe(false);
  });
});
