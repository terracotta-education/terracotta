import { describe, expect, it } from "vitest";

import { naturalCompare } from "./sort";

describe("naturalCompare", () => {
  it("orders numbers in names by value, not by their first digit", () => {
    expect(["Group 10", "Group 2", "Group 1", "Group 20", "Group 3"].sort(naturalCompare))
      .toEqual(["Group 1", "Group 2", "Group 3", "Group 10", "Group 20"]);
  });

  it("still orders names without numbers alphabetically", () => {
    expect(["Beta", "alpha", "Gamma"].sort(naturalCompare)).toEqual(["alpha", "Beta", "Gamma"]);
  });
});
