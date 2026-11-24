import { describe, expect, it } from "vitest";

import { experimentResumeRoute } from "./experiment-resume";

const conditions = [
  { name: "Condition A", defaultCondition: true },
  { name: "Condition B", defaultCondition: false }
];

const designed = {
  title: "My Experiment",
  description: "Testing worked examples",
  conditions,
  exposureType: "BETWEEN",
  participationType: "NOSET",
  distributionType: "NOSET"
};

describe("experimentResumeRoute", () => {
  it("starts a new experiment at the design intro", () => {
    expect(experimentResumeRoute({ title: "", conditions: [], exposureType: "NOSET" })).toBe("ExperimentDesignIntro");
    expect(experimentResumeRoute({ title: "   " })).toBe("ExperimentDesignIntro");
    expect(experimentResumeRoute(undefined)).toBe("ExperimentDesignIntro");
  });

  it("opens the first design step that hasn't been saved", () => {
    expect(experimentResumeRoute({ ...designed, description: "", conditions: [], exposureType: "NOSET" }))
      .toBe("ExperimentDesignDescription");
    expect(experimentResumeRoute({ ...designed, conditions: [], exposureType: "NOSET" }))
      .toBe("ExperimentDesignConditions");
    expect(experimentResumeRoute({ ...designed, conditions: [{ name: "Condition A" }, { name: "" }], exposureType: "NOSET" }))
      .toBe("ExperimentDesignConditions");
    expect(experimentResumeRoute({ ...designed, exposureType: "NOSET" }))
      .toBe("ExperimentDesignType");
    expect(experimentResumeRoute({ ...designed, conditions: conditions.map(condition => ({ ...condition, defaultCondition: false })) }))
      .toBe("ExperimentDesignDefaultCondition");
  });

  it("doesn't ask a single condition experiment for a default condition", () => {
    expect(experimentResumeRoute({ ...designed, conditions: [{ name: "Only", defaultCondition: false }] }))
      .toBe("ExperimentParticipationIntro");
  });

  it("opens participation once design is done", () => {
    expect(experimentResumeRoute(designed)).toBe("ExperimentParticipationIntro");
  });

  it("opens the first consent step that hasn't been saved", () => {
    const consent = { ...designed, participationType: "CONSENT" };

    expect(experimentResumeRoute(consent)).toBe("ParticipationTypeConsentTitle");
    expect(experimentResumeRoute({ ...consent, consent: { title: "Consent" } })).toBe("ParticipationTypeConsentFile");
    expect(experimentResumeRoute({ ...consent, consent: { title: "Consent", fileName: "consent.pdf" } })).toBe("ParticipationDistribution");
  });

  it("opens participant selection or distribution for the other selection methods", () => {
    expect(experimentResumeRoute({ ...designed, participationType: "MANUAL" })).toBe("ParticipationTypeManualSelection");
    expect(experimentResumeRoute({ ...designed, participationType: "AUTO" })).toBe("ParticipationDistribution");
  });

  it("returns null once design and participation are done", () => {
    expect(experimentResumeRoute({ ...designed, participationType: "AUTO", distributionType: "EVEN" })).toBeNull();
  });
});
