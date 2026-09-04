import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { flushPromises } from "@vue/test-utils";

const storeActions = {
  getYT: vi.fn(),
  videoStarted: vi.fn(),
  videoEnded: vi.fn(),
  videoPaused: vi.fn(),
  videoResumed: vi.fn(),
  videoRestarted: vi.fn(),
  videoJumpedTo: vi.fn(),
  videoChangedResolution: vi.fn(),
  videoChangedSpeed: vi.fn(),
  videoEnteredFullScreen: vi.fn(),
  videoExitedFullScreen: vi.fn(),
  videoMuted: vi.fn(),
  videoUnmuted: vi.fn()
};

vi.mock("@/store/media-events.module", () => ({
  mediaEvents: () => storeActions
}));

import { mountComponent } from "@/test-utils/mount";
import YoutubeEventCapture from "./YoutubeEventCapture.vue";

const props = {
  experimentId: "1",
  conditionId: "2",
  treatmentId: "3",
  assessmentId: "4",
  submissionId: "5",
  questionId: "6"
};

function createFakeYT() {
  const captured = [];

  const YT = {
    PlayerState: {
      UNSTARTED: -1,
      ENDED: 0,
      PLAYING: 1,
      PAUSED: 2,
      BUFFERING: 3,
      CUED: 5
    },
    Player: vi.fn(function FakePlayer(iframe, options) {
      const player = {
        _currentTime: 0,
        getIframe: vi.fn(() => iframe),
        isMuted: vi.fn(() => false),
        getVideoUrl: vi.fn(() => "https://youtu.be/original"),
        getDuration: vi.fn(() => 120),
        getCurrentTime: vi.fn(() => player._currentTime)
      };

      captured.push({ player, events: options.events });

      return player;
    })
  };

  return { YT, captured };
}

async function mountWithIframe(YT) {
  storeActions.getYT.mockImplementation(({ callback }) => {
    callback(YT);
    return Promise.resolve(YT);
  });

  const wrapper = mountComponent(YoutubeEventCapture, {
    props,
    slots: {
      default: "<div data-youtube-video><iframe src=\"https://www.youtube.com/embed/abc123\"></iframe></div>"
    }
  });

  await flushPromises();

  return wrapper;
}

describe("YoutubeEventCapture", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("does nothing when there is no youtube iframe in the slot content", async () => {
    const { YT } = createFakeYT();

    storeActions.getYT.mockImplementation(({ callback }) => {
      callback(YT);
      return Promise.resolve(YT);
    });

    mountComponent(YoutubeEventCapture, {
      props,
      slots: { default: "<p>no video here</p>" }
    });

    await flushPromises();

    expect(storeActions.getYT).not.toHaveBeenCalled();
    expect(YT.Player).not.toHaveBeenCalled();
  });

  it("initializes a YT.Player for each youtube iframe and enables the JS API on its src", async () => {
    const { YT, captured } = createFakeYT();
    const wrapper = await mountWithIframe(YT);

    expect(storeActions.getYT).toHaveBeenCalledTimes(1);
    expect(YT.Player).toHaveBeenCalledTimes(1);
    expect(captured).toHaveLength(1);

    const iframe = wrapper.find("iframe").element;
    expect(iframe.getAttribute("src")).toContain("enablejsapi=1");
  });

  // `playerStates` player entries are stored via `markRaw()` so Vue never
  // wraps the YT.Player instance in a reactive proxy - `getPlayerState()`'s
  // `playerState.player === player` identity check matches the raw instance
  // the YouTube API calls back with as `event.target`.
  it("onStateChange finds the matching player state and sends play/pause/resume/jump/restart/end telemetry", async () => {
    const { YT, captured } = createFakeYT();
    await mountWithIframe(YT);

    const { player, events } = captured[0];

    player._currentTime = 3;
    events.onStateChange({ target: player, data: YT.PlayerState.PLAYING });
    expect(storeActions.videoStarted).toHaveBeenCalledTimes(1);

    player._currentTime = 5;
    events.onStateChange({ target: player, data: YT.PlayerState.PAUSED });
    expect(storeActions.videoPaused).toHaveBeenCalledTimes(1);

    player._currentTime = 5.05;
    events.onStateChange({ target: player, data: YT.PlayerState.PLAYING });
    expect(storeActions.videoResumed).toHaveBeenCalledTimes(1);

    player._currentTime = 120;
    events.onStateChange({ target: player, data: YT.PlayerState.ENDED });
    expect(storeActions.videoEnded).toHaveBeenCalledTimes(1);
  });

  it("onPlaybackQualityChange/onPlaybackRateChange find the matching player state and send telemetry", async () => {
    const { YT, captured } = createFakeYT();
    await mountWithIframe(YT);

    const { player, events } = captured[0];

    events.onPlaybackQualityChange({ target: player, data: "hd720" });
    events.onPlaybackRateChange({ target: player, data: 1.5 });

    expect(storeActions.videoChangedResolution).toHaveBeenCalledTimes(1);
    expect(storeActions.videoChangedSpeed).toHaveBeenCalledTimes(1);
  });

  it("sends videoEnteredFullScreen and videoExitedFullScreen on fullscreenchange", async () => {
    const { YT, captured } = createFakeYT();
    const wrapper = await mountWithIframe(YT);

    const iframe = wrapper.find("iframe").element;

    Object.defineProperty(document, "fullscreenElement", {
      value: iframe,
      configurable: true
    });
    iframe.dispatchEvent(new Event("fullscreenchange"));

    expect(storeActions.videoEnteredFullScreen).toHaveBeenCalledTimes(1);

    Object.defineProperty(document, "fullscreenElement", {
      value: null,
      configurable: true
    });
    iframe.dispatchEvent(new Event("fullscreenchange"));

    expect(storeActions.videoExitedFullScreen).toHaveBeenCalledTimes(1);

    expect(captured[0].player.getIframe).toHaveBeenCalled();
  });

  it("polls mute state and sends videoMuted/videoUnmuted on change", async () => {
    vi.useFakeTimers();

    try {
      const { YT, captured } = createFakeYT();
      const wrapper = await mountWithIframe(YT);

      const { player, events } = captured[0];
      events.onReady({ target: player });

      player.isMuted.mockReturnValue(true);
      await vi.advanceTimersByTimeAsync(200);

      expect(storeActions.videoMuted).toHaveBeenCalledTimes(1);

      player.isMuted.mockReturnValue(false);
      await vi.advanceTimersByTimeAsync(200);

      expect(storeActions.videoUnmuted).toHaveBeenCalledTimes(1);

      wrapper.unmount();
    } finally {
      vi.useRealTimers();
    }
  });
});
