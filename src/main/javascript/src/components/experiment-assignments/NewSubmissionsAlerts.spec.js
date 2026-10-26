import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";

const service = vi.hoisted(() => ({
  getOutdated: vi.fn(),
  acknowledgeOutdated: vi.fn(),
  prepare: vi.fn(),
  poll: vi.fn(),
  retrieve: vi.fn()
}));

vi.mock("@/services/assignment-file-archive.service", () => ({
  assignmentFileArchiveService: service
}));

import { mountComponent } from "@/test-utils/mount";
import NewSubmissionsAlerts from "./NewSubmissionsAlerts.vue";

const essay = { id: "file-1", assignmentId: "assignment-1", exposureId: "exposure-1", assignmentTitle: "Essay 1", status: "OUTDATED" };
const quiz = { id: "file-2", assignmentId: "assignment-2", exposureId: "exposure-1", assignmentTitle: "Reading quiz", status: "OUTDATED" };

const mountAlerts = async () => {
  const wrapper = mountComponent(NewSubmissionsAlerts, { props: { experimentId: "experiment-1" } });
  await flushPromises();

  return wrapper;
};

describe("NewSubmissionsAlerts", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    Object.values(service).forEach(fn => fn.mockReset());
    service.getOutdated.mockResolvedValue([essay, quiz]);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("shows one alert per assignment with new submissions, naming it", async () => {
    const wrapper = await mountAlerts();

    expect(service.getOutdated).toHaveBeenCalledWith("experiment-1");
    const alerts = wrapper.findAll(".new-submissions-alert");
    expect(alerts).toHaveLength(2);
    expect(alerts[0].text()).toContain("There have been new submissions for Essay 1 since you last downloaded the file archive for this assignment.");
    expect(alerts[0].text()).toContain("Click here to download a new archive.");
    expect(alerts[1].text()).toContain("Reading quiz");
  });

  it("shows nothing when no assignment has new submissions", async () => {
    service.getOutdated.mockResolvedValue([]);
    const wrapper = await mountAlerts();

    expect(wrapper.find(".new-submissions-alerts").exists()).toBe(false);
  });

  it("shows nothing when the list can't be loaded", async () => {
    service.getOutdated.mockResolvedValue({ status: 500, error: "boom" });
    const wrapper = await mountAlerts();

    expect(wrapper.find(".new-submissions-alerts").exists()).toBe(false);
  });

  it("dismisses an alert and keeps it dismissed until there are newer submissions", async () => {
    const wrapper = await mountAlerts();

    await wrapper.findAll(".new-submissions-alert")[0].find("button").trigger("click");

    expect(service.acknowledgeOutdated).toHaveBeenCalledWith("experiment-1", "exposure-1", "assignment-1", "file-1");
    expect(wrapper.findAll(".new-submissions-alert")).toHaveLength(1);
    expect(wrapper.text()).not.toContain("Essay 1");
  });

  it("prepares a new archive, downloads it once it's ready and removes the alert", async () => {
    service.prepare.mockResolvedValue({ id: "file-3", status: "PROCESSING" });
    const ready = { id: "file-3", status: "READY", fileName: "essay.zip" };
    service.poll.mockResolvedValueOnce({ id: "file-3", status: "PROCESSING" }).mockResolvedValueOnce(ready);
    const wrapper = await mountAlerts();

    await wrapper.findAll(".new-submissions-alert")[0].find("a").trigger("click");
    await flushPromises();

    expect(service.prepare).toHaveBeenCalledWith("experiment-1", "exposure-1", "assignment-1");
    expect(wrapper.text()).toContain("Preparing a new file archive for Essay 1. It downloads when it's ready.");
    expect(wrapper.findAll(".new-submissions-alert")[0].classes()).toContain("text-info");

    await vi.advanceTimersByTimeAsync(5000);
    expect(service.retrieve).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(5000);
    expect(service.poll).toHaveBeenCalledWith("experiment-1", "exposure-1", "assignment-1", false);
    expect(service.retrieve).toHaveBeenCalledWith("experiment-1", "exposure-1", "assignment-1", ready);
    expect(wrapper.text()).not.toContain("Essay 1");
    expect(wrapper.findAll(".new-submissions-alert")).toHaveLength(1);
  });

  it("offers a retry when preparing the archive fails", async () => {
    service.prepare.mockResolvedValue({ id: "file-3", status: "PROCESSING" });
    service.poll.mockResolvedValue({ id: "file-3", status: "ERROR" });
    const wrapper = await mountAlerts();

    await wrapper.findAll(".new-submissions-alert")[0].find("a").trigger("click");
    await vi.advanceTimersByTimeAsync(5000);

    const alert = wrapper.findAll(".new-submissions-alert")[0];
    expect(alert.text()).toContain("There was an error preparing the new file archive for Essay 1.");
    expect(alert.find("a").text()).toContain("Click here to try again");
    expect(service.retrieve).not.toHaveBeenCalled();

    // the failed archive stops being polled
    await vi.advanceTimersByTimeAsync(10000);
    expect(service.poll).toHaveBeenCalledTimes(1);
  });

  it("shows the error at once when the archive can't be requested", async () => {
    service.prepare.mockResolvedValue({ status: 500, error: "boom" });
    const wrapper = await mountAlerts();

    await wrapper.findAll(".new-submissions-alert")[0].find("a").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("There was an error preparing the new file archive for Essay 1.");
    expect(service.poll).not.toHaveBeenCalled();
  });

  it("stops polling when the page is left", async () => {
    service.prepare.mockResolvedValue({ id: "file-3", status: "PROCESSING" });
    service.poll.mockResolvedValue({ id: "file-3", status: "PROCESSING" });
    const wrapper = await mountAlerts();

    await wrapper.findAll(".new-submissions-alert")[0].find("a").trigger("click");
    await flushPromises();
    wrapper.unmount();
    await vi.advanceTimersByTimeAsync(20000);

    expect(service.poll).not.toHaveBeenCalled();
  });
});
