import { describe, expect, it, vi } from "vitest";

import { LMS_REAUTHORIZATION_HEADER, watchForLmsReauthorization } from "./lms-reauthorization";

const response = flagged => ({
  headers: new Headers(flagged ? { [LMS_REAUTHORIZATION_HEADER]: "true" } : {})
});

const setup = ({ isInstructor = true, responses }) => {
  const target = { fetch: vi.fn() };
  responses.forEach(r => target.fetch.mockResolvedValueOnce(r));
  const notify = vi.fn();

  watchForLmsReauthorization({ target, isInstructor: () => isInstructor, notify });

  return { target, notify };
};

describe("watchForLmsReauthorization", () => {
  it("tells an instructor when a response says their LMS connection needs reauthorizing", async () => {
    const { target, notify } = setup({ responses: [response(true)] });

    await target.fetch("/api/experiments");

    expect(notify).toHaveBeenCalledTimes(1);
  });

  // several requests on one screen can all fail on the same dead token
  it("tells them only once per page load", async () => {
    const { target, notify } = setup({ responses: [response(true), response(true)] });

    await target.fetch("/api/a");
    await target.fetch("/api/b");

    expect(notify).toHaveBeenCalledTimes(1);
  });

  // a student's request can fail on the instructor's token, which the student can't fix
  it("never tells a student", async () => {
    const { target, notify } = setup({ isInstructor: false, responses: [response(true)] });

    await target.fetch("/api/experiments");

    expect(notify).not.toHaveBeenCalled();
  });

  it("stays quiet for ordinary responses and passes them through unchanged", async () => {
    const plain = response(false);
    const { target, notify } = setup({ responses: [plain] });

    await expect(target.fetch("/api/experiments")).resolves.toBe(plain);
    expect(notify).not.toHaveBeenCalled();
  });
});
