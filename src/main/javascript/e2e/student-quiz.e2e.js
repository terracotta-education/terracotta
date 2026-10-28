import { expect, test } from "@playwright/test";

import { mockApi } from "./support/api.js";
import { openApp, signIn } from "./support/session.js";

const EXPERIMENT_ID = "exp-quiz";

const LAUNCH = {
  experimentId: EXPERIMENT_ID,
  conditionId: 1,
  treatmentId: 2,
  assessmentId: 3,
  submissionId: 4,
  questionSubmissionDtoList: []
};

const ASSESSMENT = {
  assessmentId: 3,
  questions: [
    {
      questionId: 10,
      questionOrder: 0,
      questionType: "MC",
      html: "<p>Favorite dog?</p>",
      points: 1,
      answers: [
        { answerId: "100", html: "<p>Collie</p>", answerOrder: 0 },
        { answerId: "101", html: "<p>Beagle</p>", answerOrder: 1 },
        { answerId: "102", html: "<p>Poodle</p>", answerOrder: 2 }
      ]
    }
  ]
};

// a student opening an assignment they haven't attempted yet
async function openQuiz(page) {
  await mockApi(page, [
    [`/api/experiments/${EXPERIMENT_ID}/conditions/1/treatments/2/assessments/3`, ASSESSMENT]
  ]);

  // the steps share one path; which one it is comes in the body
  await page.route(`**/api/experiments/${EXPERIMENT_ID}/step**`, route => {
    const { step } = route.request().postDataJSON();
    const body = step === "launch_assignment"
      ? LAUNCH
      : { retakeDetails: { retakeAllowed: true, submissionAttemptsCount: 0 }, submissions: [], maxPoints: 1 };

    return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
  });

  await signIn(page, { as: "student", experimentId: EXPERIMENT_ID, lmsAssignmentId: "lms-assignment-1" });
  await openApp(page);
}

// reported by a screen reader (JAWS) user: the arrow keys didn't change the answer, and the chosen
// answer wasn't read back as selected
test("a multiple choice question is one group of options that the arrow keys move through", async ({ page }) => {
  await openQuiz(page);

  const question = page.getByRole("radiogroup", { name: "Favorite dog?" });
  await expect(question.getByRole("radio")).toHaveCount(3);

  await question.getByRole("radio", { name: "Collie" }).check();
  await expect(question.getByRole("radio", { name: "Collie" })).toBeChecked();

  await page.keyboard.press("ArrowDown");
  await expect(question.getByRole("radio", { name: "Beagle" })).toBeChecked();
  await expect(question.getByRole("radio", { name: "Collie" })).not.toBeChecked();

  await page.keyboard.press("ArrowUp");
  await expect(question.getByRole("radio", { name: "Collie" })).toBeChecked();
});
