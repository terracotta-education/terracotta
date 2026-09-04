import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";

vi.mock("@/services", () => ({
  mediaEventsService: {
    createVideoEvent: vi.fn()
  }
}));

import { mediaEventsService } from "@/services";
import { mediaEvents } from "./media-events.module";

describe("mediaEvents store", () => {
  let store;

  beforeEach(() => {
    setActivePinia(createPinia());
    store = mediaEvents();
    vi.clearAllMocks();
  });

  it("videoActions getter exposes the action-name-to-verb map", () => {
    expect(store.videoActions).toMatchObject({
      videoStarted: "Started",
      videoEnded: "Ended",
      videoPaused: "Paused",
      videoResumed: "Resumed",
      videoRestarted: "Restarted",
      videoJumpedTo: "JumpedTo",
      videoChangedResolution: "ChangedResolution",
      videoChangedSpeed: "ChangedSpeed",
      videoEnteredFullScreen: "EnteredFullScreen",
      videoExitedFullScreen: "ExitedFullScreen",
      videoMuted: "Muted",
      videoUnmuted: "Unmuted"
    });
  });

  describe("video event actions", () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date("2026-01-01T00:00:00.000Z"));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it("videoStarted builds and sends a MediaEvent with the Started action", async () => {
      mediaEventsService.createVideoEvent.mockResolvedValue({ status: 200 });

      const result = await store.videoStarted({
        experimentId: 1,
        conditionId: 2,
        treatmentId: 3,
        assessmentId: 4,
        submissionId: 5,
        questionId: 6,
        originalVideoUrl: "https://youtu.be/abc",
        videoURL: "https://youtu.be/abc?t=10",
        duration: 120,
        currentTime: 10
      });

      expect(mediaEventsService.createVideoEvent).toHaveBeenCalledWith({
        experimentId: 1,
        conditionId: 2,
        treatmentId: 3,
        assessmentId: 4,
        submissionId: 5,
        questionId: 6,
        event: {
          type: "MediaEvent",
          profile: "MediaProfile",
          action: "Started",
          object: {
            id: "https://youtu.be/abc",
            type: "VideoObject",
            mediaType: "video/vnd.youtube.yt",
            duration: "PT2M"
          },
          target: {
            id: "https://youtu.be/abc?t=10",
            type: "MediaLocation",
            currentTime: "PT10S"
          },
          eventTime: "2026-01-01T00:00:00.000Z"
        }
      });
      expect(result).toEqual({ status: 200 });
    });

    it("videoJumpedTo includes extensions when provided", async () => {
      mediaEventsService.createVideoEvent.mockResolvedValue({ status: 200 });

      await store.videoJumpedTo({
        experimentId: 1,
        conditionId: 2,
        treatmentId: 3,
        assessmentId: 4,
        submissionId: 5,
        questionId: 6,
        originalVideoUrl: "https://youtu.be/abc",
        videoURL: "https://youtu.be/abc",
        duration: 60,
        currentTime: 30,
        extensions: { from: 5, to: 30 }
      });

      const [[callArg]] = mediaEventsService.createVideoEvent.mock.calls;

      expect(callArg.event.action).toBe("JumpedTo");
      expect(callArg.event.extensions).toEqual({ from: 5, to: 30 });
    });

    it("omits extensions entirely when not provided", async () => {
      mediaEventsService.createVideoEvent.mockResolvedValue({ status: 200 });

      await store.videoPaused({
        experimentId: 1,
        conditionId: 2,
        treatmentId: 3,
        assessmentId: 4,
        submissionId: 5,
        questionId: 6,
        originalVideoUrl: "https://youtu.be/abc",
        videoURL: "https://youtu.be/abc",
        duration: 60,
        currentTime: 30
      });

      const [[callArg]] = mediaEventsService.createVideoEvent.mock.calls;

      expect(callArg.event).not.toHaveProperty("extensions");
    });

    it("logs and swallows a rejection from the service, returning null", async () => {
      const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
      mediaEventsService.createVideoEvent.mockRejectedValue(
        new Error("boom")
      );

      const result = await store.videoEnded({
        experimentId: 1,
        conditionId: 2,
        treatmentId: 3,
        assessmentId: 4,
        submissionId: 5,
        questionId: 6,
        originalVideoUrl: "https://youtu.be/abc",
        videoURL: "https://youtu.be/abc",
        duration: 60,
        currentTime: 30
      });

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });
  });

  describe("getYT", () => {
    it("injects the iframe API script tag and resolves once the global callback fires", async () => {
      const script = document.createElement("script");
      document.body.appendChild(script);

      const callback = vi.fn();
      const fakeYT = { loaded: true };

      const promise = store.getYT({ callback });

      const injectedScript = document.getElementById("iframe-api");
      expect(injectedScript).not.toBeNull();
      expect(injectedScript.src).toBe("https://www.youtube.com/iframe_api");

      window.YT = fakeYT;
      window.onYouTubeIframeAPIReady();

      const result = await promise;

      expect(result).toBe(fakeYT);
      expect(callback).toHaveBeenCalledWith(fakeYT);
    });

    it("returns the cached API on a later call without injecting a second script tag", async () => {
      const scriptsBefore =
        document.querySelectorAll("#iframe-api").length;

      const result = await store.getYT();

      expect(result).toBe(window.YT);
      expect(document.querySelectorAll("#iframe-api").length).toBe(
        scriptsBefore
      );
    });

    it("works without a callback argument", async () => {
      await expect(store.getYT()).resolves.toBe(window.YT);
    });
  });
});
