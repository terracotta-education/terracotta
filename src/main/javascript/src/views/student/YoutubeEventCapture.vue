<template>
  <div ref="rootElement">
    <slot />
  </div>
</template>

<script setup>
import {
  ref,
  onMounted,
  onBeforeUnmount,
  nextTick,
  markRaw
} from "vue";

import { mediaEvents as mediaEventsModule } from "@/store/media-events.module";

defineOptions({
  name: "YoutubeEventCapture"
});

const props = defineProps({
  experimentId: {
    type: [String, Number],
    required: true
  },
  conditionId: {
    type: [String, Number],
    required: true
  },
  treatmentId: {
    type: [String, Number],
    required: true
  },
  assessmentId: {
    type: [String, Number],
    required: true
  },
  submissionId: {
    type: [String, Number],
    required: true
  },
  questionId: {
    type: [String, Number],
    required: true
  }
});

const mediaEventsStore = mediaEventsModule();

const rootElement = ref(null);
const playerStates = ref([]);
const intervalId = ref(null);

const getYoutubeIframes = () => {
  return rootElement.value?.querySelectorAll(
    "div[data-youtube-video] > iframe"
  ) || [];
};

const getPlayerState = player => {
  return playerStates.value.find(
    playerState => playerState.player === player
  ) || null;
};

const getPlayerStateByIframe = iframe => {
  return playerStates.value.find(
    playerState => playerState.player.getIframe() === iframe
  ) || null;
};

const youtubeIframeAPIInit = YT => {
  const allYoutubeIframes = getYoutubeIframes();

  for (const iframe of allYoutubeIframes) {
    if (iframe.getAttribute("src")) {
      const src = new URL(iframe.getAttribute("src"));
      src.searchParams.set("enablejsapi", 1);
      iframe.setAttribute("src", src.toString());
    }

    const player = new YT.Player(iframe, {
      events: {
        onReady: event => {
          intervalId.value = window.setInterval(
            pollApiInfo,
            200
          );

          const state = getPlayerState(event.target);

          if (!state) {
            return;
          }

          state.muted = state.player.isMuted();
          state.originalVideoUrl = state.player.getVideoUrl();
        },

        onStateChange: event => {
          const state = getPlayerState(event.target);

          if (!state) {
            return;
          }

          if (
            event.data === YT.PlayerState.PLAYING &&
            !state.started
          ) {
            state.started = true;
            onVideoStarted(state);
          } else if (
            event.data === YT.PlayerState.ENDED
          ) {
            onVideoEnded(state);
          } else if (
            event.data === YT.PlayerState.PAUSED
          ) {
            onVideoPaused(state);
          } else if (
            event.data === YT.PlayerState.PLAYING &&
            state.started
          ) {
            if (
              state.started &&
              state.player.getCurrentTime() < 1 &&
              state.player.getCurrentTime() < state.currentTime
            ) {
              onVideoRestarted(state);
            } else if (
              state.currentTime > 0 &&
              Math.abs(
                state.currentTime -
                  state.player.getCurrentTime()
              ) > 0.1
            ) {
              onVideoJumpedTo(state);
            } else {
              onVideoResumed(state);
            }
          }
        },

        onPlaybackQualityChange: event => {
          const state = getPlayerState(event.target);

          if (state) {
            onChangedResolution(state, event.data);
          }
        },

        onPlaybackRateChange: event => {
          const state = getPlayerState(event.target);

          if (state) {
            onChangedSpeed(state, event.data);
          }
        }
      }
    });

    iframe.addEventListener(
      "fullscreenchange",
      onFullscreenChange
    );

    playerStates.value.push({
      // markRaw prevents Vue from wrapping the YT.Player instance in a reactive
      // proxy - without it, playerState.player !== the raw player reference
      // handed back by the YouTube API in event.target, breaking every
      // getPlayerState() lookup by identity.
      player: markRaw(player),
      started: false,
      currentTime: -1,
      muted: false,
      originalVideoUrl: null
    });
  }
};

const sendVideoEvent = (
  storeAction,
  {
    player,
    originalVideoUrl,
    extensions = null
  }
) => {
  mediaEventsStore[storeAction]({
    experimentId: props.experimentId,
    conditionId: props.conditionId,
    treatmentId: props.treatmentId,
    assessmentId: props.assessmentId,
    submissionId: props.submissionId,
    questionId: props.questionId,
    originalVideoUrl,
    videoURL: player.getVideoUrl(),
    duration: player.getDuration(),
    currentTime: player.getCurrentTime(),
    extensions
  });
};

const onVideoStarted = playerState => {
  playerState.currentTime = playerState.player.getCurrentTime();
  sendVideoEvent("videoStarted", playerState);
};

const onVideoEnded = playerState => {
  playerState.currentTime = playerState.player.getCurrentTime();
  sendVideoEvent("videoEnded", playerState);
};

const onVideoPaused = playerState => {
  playerState.currentTime = playerState.player.getCurrentTime();
  sendVideoEvent("videoPaused", playerState);
};

const onVideoResumed = playerState => {
  playerState.currentTime = playerState.player.getCurrentTime();
  sendVideoEvent("videoResumed", playerState);
};

const onVideoRestarted = playerState => {
  playerState.currentTime = playerState.player.getCurrentTime();
  sendVideoEvent("videoRestarted", playerState);
};

const onVideoJumpedTo = playerState => {
  playerState.currentTime = playerState.player.getCurrentTime();
  sendVideoEvent("videoJumpedTo", playerState);
};

const onChangedResolution = (
  playerState,
  resolution
) => {
  const resolutions = {
    tiny: "144",
    small: "240",
    medium: "360",
    large: "480",
    hd720: "720",
    hd1080: "1080",
    hd1440: "1440",
    hd2160: "2160"
  };

  playerState.currentTime =
    playerState.player.getCurrentTime();

  sendVideoEvent(
    "videoChangedResolution",
    {
      ...playerState,
      extensions: {
        resolution:
          resolutions?.[resolution] ||
          resolution
      }
    }
  );
};

const onChangedSpeed = (
  playerState,
  speed
) => {
  playerState.currentTime =
    playerState.player.getCurrentTime();

  sendVideoEvent(
    "videoChangedSpeed",
    {
      ...playerState,
      extensions: {
        speed
      }
    }
  );
};

const onFullscreenChange = event => {
  const playerState =
    getPlayerStateByIframe(event.target);

  if (!playerState) {
    return;
  }

  playerState.currentTime =
    playerState.player.getCurrentTime();

  if (document.fullscreenElement) {
    sendVideoEvent(
      "videoEnteredFullScreen",
      playerState
    );
  } else {
    sendVideoEvent(
      "videoExitedFullScreen",
      playerState
    );
  }
};

const onMuted = playerState => {
  playerState.currentTime =
    playerState.player.getCurrentTime();
  playerState.muted = true;

  sendVideoEvent("videoMuted", playerState);
};

const onUnmuted = playerState => {
  playerState.currentTime =
    playerState.player.getCurrentTime();
  playerState.muted = false;

  sendVideoEvent("videoUnmuted", playerState);
};

const pollApiInfo = () => {
  for (const playerState of playerStates.value) {
    const muted = playerState.player.isMuted();

    if (muted && !playerState.muted) {
      onMuted(playerState);
    } else if (!muted && playerState.muted) {
      onUnmuted(playerState);
    }
  }
};

onMounted(async () => {
  await nextTick();

  if (getYoutubeIframes().length > 0) {
    mediaEventsStore.getYT({
      callback: youtubeIframeAPIInit
    });
  }
});

onBeforeUnmount(() => {
  const allYoutubeIframes = getYoutubeIframes();

  for (const iframe of allYoutubeIframes) {
    iframe.removeEventListener(
      "fullscreenchange",
      onFullscreenChange
    );
  }

  if (intervalId.value) {
    window.clearInterval(intervalId.value);
  }
});
</script>
