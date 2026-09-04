import { describe, expect, it } from "vitest";

import { mountComponent } from "@/test-utils/mount";
import Spinner from "./Spinner.vue";

describe("Spinner", () => {
  it("renders an svg with the default size and circle geometry", () => {
    const wrapper = mountComponent(Spinner);

    const svg = wrapper.find("svg.spinner");

    expect(svg.exists()).toBe(true);
    expect(svg.attributes("width")).toBe("28px");
    expect(svg.attributes("height")).toBe("28px");
    expect(svg.attributes("viewBox")).toBe("0 0 66 66");

    const circle = wrapper.find("circle.path");

    expect(circle.attributes("cx")).toBe("33");
    expect(circle.attributes("cy")).toBe("33");
    expect(circle.attributes("r")).toBe("30");
  });

  it("respects custom size and geometry props", () => {
    const wrapper = mountComponent(Spinner, {
      props: {
        width: "100px",
        height: "100px",
        viewBox: "0 0 10 10",
        cx: "5",
        cy: "5",
        r: "4"
      }
    });

    const svg = wrapper.find("svg.spinner");

    expect(svg.attributes("width")).toBe("100px");
    expect(svg.attributes("height")).toBe("100px");
    expect(svg.attributes("viewBox")).toBe("0 0 10 10");

    const circle = wrapper.find("circle.path");

    expect(circle.attributes("cx")).toBe("5");
    expect(circle.attributes("cy")).toBe("5");
    expect(circle.attributes("r")).toBe("4");
  });
});
