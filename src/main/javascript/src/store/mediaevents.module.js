import { mediaEventsService } from "@/services";
import moment from "moment";

// Youtube IFrame API

let youtubeIFrameAPI = null;
let isLoadingYoutubeIFrameAPI = false;

const state = {};

const loadYoutubeIframeAPI = function() {
  isLoadingYoutubeIFrameAPI = true;
  var tag = document.createElement("script");
  tag.id = "iframe-api";
  tag.src = "https://www.youtube.com/iframe_api";
  var firstScriptTag = document.getElementsByTagName("script")[0];
  firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
  window.onYouTubeIframeAPIReady = () => {
    youtubeIFrameAPI = window.YT;
    isLoadingYoutubeIFrameAPI = false;
  };
};

const sendEvent = function({
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
  extensions,
}) {
  const event = {
    type: "MediaEvent",
    profile: "MediaProfile",
    action: action,
    object: {
      id: originalVideoUrl,
      type: "VideoObject",
      mediaType: "video/vnd.youtube.yt",
      duration: moment.duration(duration, "s").toISOString(),
    },
    target: {
      id: videoURL,
      type: "MediaLocation",
      currentTime: moment.duration(currentTime, "s").toISOString(),
    },
    eventTime: new Date().toISOString(),
  };
  if (extensions) {
    event.extensions = extensions;
  }
  // Fire and forget
  mediaEventsService.createVideoEvent({
    experimentId,
    conditionId,
    treatmentId,
    assessmentId,
    submissionId,
    questionId,
    event,
  });
};

const actions = {
  getYT({ commit }, { callback }) {
    if (!youtubeIFrameAPI && !isLoadingYoutubeIFrameAPI) {
      loadYoutubeIframeAPI({ commit });
    }
    function checkForYTLibrary() {
      if (youtubeIFrameAPI) {
        callback(youtubeIFrameAPI);
      } else {
        setTimeout(checkForYTLibrary, 50);
      }
    }
    checkForYTLibrary();
  },
  videoStarted(context, payload) {
    sendEvent({ ...payload, action: "Started" });
  },
  videoEnded(context, payload) {
    sendEvent({ ...payload, action: "Ended" });
  },
  videoPaused(context, payload) {
    sendEvent({ ...payload, action: "Paused" });
  },
  videoResumed(context, payload) {
    sendEvent({ ...payload, action: "Resumed" });
  },
  videoRestarted(context, payload) {
    sendEvent({ ...payload, action: "Restarted" });
  },
  videoJumpedTo(context, payload) {
    sendEvent({ ...payload, action: "JumpedTo" });
  },
  videoChangedResolution(context, payload) {
    sendEvent({ ...payload, action: "ChangedResolution" });
  },
  videoChangedSpeed(context, payload) {
    sendEvent({ ...payload, action: "ChangedSpeed" });
  },
  videoEnteredFullScreen(context, payload) {
    sendEvent({ ...payload, action: "EnteredFullScreen" });
  },
  videoExitedFullScreen(context, payload) {
    sendEvent({ ...payload, action: "ExitedFullScreen" });
  },
  videoMuted(context, payload) {
    sendEvent({ ...payload, action: "Muted" });
  },
  videoUnmuted(context, payload) {
    sendEvent({ ...payload, action: "Unmuted" });
  },
};

const mutations = {};

const getters = {};

export const mediaevents = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
};
