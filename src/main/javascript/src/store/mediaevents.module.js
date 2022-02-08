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

const actions = {
  getYT({ commit, state }, { callback }) {
      console.log("getYT", state.youtubeIFrameAPI, state.isLoadingYoutubeIFrameAPI);
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
  videoStarted(
    context,
    {
      experiment_id,
      condition_id,
      treatment_id,
      assessment_id,
      submission_id,
      question_id,
      videoURL,
      duration,
      currentTime,
    }
  ) {
    const event = {
      type: "MediaEvent",
      profile: "MediaProfile",
      action: "Started",
      object: {
        id: videoURL,
        type: "VideoObject",
        mediaType: "video/vnd.youtube.yt",
        duration: moment.duration(duration, 's').toISOString(),
        extensions: {
          terracotta_question_id: question_id,
          terracotta_submission_id: submission_id,
        },
      },
      target: {
        id: `${videoURL}?t=${Math.floor(duration)}s`,
        type: "MediaLocation",
        currentTime: moment.duration(currentTime, 's').toISOString(),
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
      event,
    });
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
