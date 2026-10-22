// A started two-condition experiment with one exposure set of three assignments, as the API
// returns it. Each assignment has a treatment per condition with nothing built yet.
export const EXPERIMENT_ID = "exp-1";
export const EXPOSURE_ID = "x1";

export const conditions = [
  { conditionId: "c1", name: "Condition A", defaultCondition: true },
  { conditionId: "c2", name: "Condition B", defaultCondition: false }
];

export const experiment = {
  experimentId: EXPERIMENT_ID,
  title: "Reading strategies study",
  description: "",
  exposureType: "BETWEEN",
  participationType: "AUTO",
  conditions,
  started: "2026-10-01T00:00:00Z",
  closed: null,
  distributionType: "EVEN",
  consentDocumentDto: null
};

const treatment = (treatmentId, conditionId) => ({
  treatmentId,
  conditionId,
  assessmentDto: { assessmentId: `${treatmentId}-assessment`, questions: [] }
});

export const assignments = [1, 2, 3].map(number => ({
  assignmentId: `a${number}`,
  exposureId: EXPOSURE_ID,
  title: `Assignment ${number}`,
  published: number !== 3,
  dueDate: null,
  assignmentOrder: number,
  treatments: [treatment(`t${number}a`, "c1"), treatment(`t${number}b`, "c2")]
}));

export const configuration = {
  lmsTitle: "Canvas",
  messagingEnabled: true,
  experimentExportEnabled: true
};

// the responses the experiment pages need, for mockApi
export const experimentResponses = [
  [`/api/experiments/${EXPERIMENT_ID}`, experiment],
  [`/api/experiments/${EXPERIMENT_ID}/exposures`, [{ exposureId: EXPOSURE_ID, title: "Set 1", experimentId: EXPERIMENT_ID }]],
  [new RegExp(`/exposures/${EXPOSURE_ID}/assignments$`), assignments],
  [/\/conditions$/, conditions],
  ["/api/configuration", configuration]
];
