import { defineStore } from "pinia";

import { mediaEventsService } from "@/services";
import dayjs from "@/plugins/dayjs";

let youtubeIFrameAPI = null;
let youtubeIFrameAPIPromise = null;

const VIDEO_ACTIONS = Object.freeze({
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

const loadYoutubeIframeAPI = () => {
  if (youtubeIFrameAPI) {
    return Promise.resolve(youtubeIFrameAPI);
  }

  if (youtubeIFrameAPIPromise) {
    return youtubeIFrameAPIPromise;
  }

  youtubeIFrameAPIPromise = new Promise(resolve => {
    const existingScript = document.getElementById("iframe-api");

    window.onYouTubeIframeAPIReady = () => {
      youtubeIFrameAPI = window.YT;
      resolve(youtubeIFrameAPI);
    };

    if (existingScript) {
      return;
    }

    const tag = document.createElement("script");
    tag.id = "iframe-api";
    tag.src = "https://www.youtube.com/iframe_api";

    const firstScriptTag = document.getElementsByTagName("script")[0];

    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
  });

  return youtubeIFrameAPIPromise;
};

const sendEvent = async ({
  experimentId,
  conditionId,
  treatmentId,
  assessmentId,
  submissionId,
  questionId,
  originalVideoUrl,
  videoURL,
  duration,
  currentTime,
  action,
  extensions
}) => {
  try {
    const event = {
      type: "MediaEvent",
      profile: "MediaProfile",
      action,
      object: {
        id: originalVideoUrl,
        type: "VideoObject",
        mediaType: "video/vnd.youtube.yt",
        duration: dayjs.duration(duration, "s").toISOString()
      },
      target: {
        id: videoURL,
        type: "MediaLocation",
        currentTime: dayjs.duration(currentTime, "s").toISOString()
      },
      eventTime: new Date().toISOString(),
      ...(extensions ? { extensions } : {})
    };

    return await mediaEventsService.createVideoEvent({
      experimentId,
      conditionId,
      treatmentId,
      assessmentId,
      submissionId,
      questionId,
      event
    });
  } catch (error) {
    console.error("mediaEvents/sendEvent | catch", error);

    return null;
  }
};

export const mediaEvents = defineStore("mediaEvents", {
  state: () => ({}),

  getters: {
    videoActions: () => VIDEO_ACTIONS
  },

  actions: {
    async getYT({ callback } = {}) {
      const YT = await loadYoutubeIframeAPI();

      if (typeof callback === "function") {
        callback(YT);
      }

      return YT;
    },

    ...Object.fromEntries(
      Object.entries(VIDEO_ACTIONS).map(([actionName, action]) => [
        actionName,
        payload => sendEvent({ ...payload, action })
      ])
    )
  }
});
