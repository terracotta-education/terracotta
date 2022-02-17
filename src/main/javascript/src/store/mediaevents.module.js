import { mediaEventsService } from "@/services";
import moment from "moment";

// Youtube IFrame API

const state = {
  youtubeIFrameAPI: null,
  isLoadingYoutubeIFrameAPI: false,
};

const loadYoutubeIframeAPI = function({ commit }) {
  commit("setIsLoadingYoutubeIFrameAPI", { isLoadingYoutubeIFrameAPI: true });
  var tag = document.createElement("script");
  tag.id = "iframe-api";
  tag.src = "https://www.youtube.com/iframe_api";
  var firstScriptTag = document.getElementsByTagName("script")[0];
  firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
  window.onYouTubeIframeAPIReady = () => {
    commit("setYoutubeIFrameAPI", { youtubeIFrameAPI: window.YT });
    commit("setIsLoadingYoutubeIFrameAPI", {
      isLoadingYoutubeIFrameAPI: false,
    });
  };
};

const sendEvent = function({
  experiment_id,
  condition_id,
  treatment_id,
  assessment_id,
  submission_id,
  question_id,
  originalVideoUrl,
  videoURL,
  duration,
  currentTime,
  action,
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
  // Fire and forget
  mediaEventsService.createVideoEvent({
    experiment_id,
    condition_id,
    treatment_id,
    assessment_id,
    submission_id,
    question_id,
    event,
  });
};

const actions = {
  getYT({ commit, state }, { callback }) {
    if (!state.youtubeIFrameAPI && !state.isLoadingYoutubeIFrameAPI) {
      loadYoutubeIframeAPI({ commit });
    }
    function checkForYTLibrary() {
      if (state.youtubeIFrameAPI) {
        callback(state.youtubeIFrameAPI);
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

const mutations = {
  setYoutubeIFrameAPI(state, { youtubeIFrameAPI }) {
    state.youtubeIFrameAPI = youtubeIFrameAPI;
  },
  setIsLoadingYoutubeIFrameAPI(state, { isLoadingYoutubeIFrameAPI }) {
    state.isLoadingYoutubeIFrameAPI = isLoadingYoutubeIFrameAPI;
  },
};

const getters = {};

export const mediaevents = {
  namespaced: true,
  state,
  actions,
  mutations,
  getters,
};
