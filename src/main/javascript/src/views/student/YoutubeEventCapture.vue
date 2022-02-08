<template>
  <div>
    <slot />
  </div>
</template>

<script>
import { mapActions } from "vuex";

const STATES = {
  NOT_STARTED: 0,
  STARTED: 1,
  ENDED: 2,
  PAUSED: 3,
  RESUMED: 4,
  RESTARTED: 5,
  JUMPED_TO: 6,
  CHANGED_RESOLUTION: 7,
  CHANGED_SPEED: 8,
  ENTERED_FULLSCREEN: 9,
  EXITED_FULLSCREEN: 10,
  MUTED: 11,
  UNMUTED: 12,
};
export default {
  // TODO: is there any way to get these, except questionId, from the store?
  props: [
    "experimentId",
    "conditionId",
    "treatmentId",
    "assessmentId",
    "submissionId",
    "questionId",
  ],
  data() {
    return {
      playerStates: [],
      intervalId: null,
    };
  },
  methods: {
    ...mapActions("mediaevents", ["getYT", "videoStarted"]),
    getPlayerState(player) {
      for (const playerState of this.playerStates) {
        if (playerState.player === player) {
          return playerState;
        }
      }
      return null;
    },
    getPlayerStateByIframe(iframe) {
      for (const playerState of this.playerStates) {
        if (playerState.player.getIframe() === iframe) {
          return playerState;
        }
      }
      return null;
    },
    youtubeIframeAPIInit(YT) {
      const allYoutubeIframes = this.$el.querySelectorAll(
        "iframe[data-youtube-id]"
      );
      for (const iframe of allYoutubeIframes) {
        const player = new YT.Player(iframe, {
          events: {
            onReady: (event) => {
              this.intervalId = setInterval(this.pollApiInfo, 200);
              const state = this.getPlayerState(event.target);
              state.muted = state.player.isMuted();
            },
            onStateChange: (event) => {
              const state = this.getPlayerState(event.target);
              if (event.data === 1 && !state.started) {
                state.started = true;
                this.onVideoStarted(state);
              } else if (event.data === 0) {
                this.onVideoEnded(state);
              } else if (event.data === 2) {
                this.onVideoPaused(state);
              } else if (event.data === 1 && state.started) {
                // Try to determine if restarted by seeing if the current time
                // is less than 1s and less than previous current time
                if (
                  state.started &&
                  state.player.getCurrentTime() < 1 &&
                  state.player.getCurrentTime() < state.currentTime
                ) {
                  this.onVideoRestarted(state);
                } else if (
                  state.currentTime > 0 &&
                  Math.abs(state.currentTime - state.player.getCurrentTime()) >
                    0.1
                ) {
                  this.onVideoJumpedTo(state);
                } else {
                  this.onVideoResumed(state);
                }
              }
            },
            onPlaybackQualityChange: (event) => {
              const state = this.getPlayerState(event.target);
              this.onChangedResolution(state, event.data);
            },
            onPlaybackRateChange: (event) => {
              const state = this.getPlayerState(event.target);
              this.onChangedSpeed(state, event.data);
            },
          },
        });
        iframe.addEventListener("fullscreenchange", this.onFullscreenChange);
        this.playerStates.push({
          player,
          started: false,
          currentState: STATES.NOT_STARTED,
          currentTime: -1,
          muted: false,
        });
      }
    },
    onVideoStarted(playerState) {
      playerState.currentState = STATES.STARTED;
      playerState.currentTime = playerState.player.getCurrentTime();
      this.videoStarted({
        experiment_id: this.experimentId,
        condition_id: this.conditionId,
        treatment_id: this.treatmentId,
        assessment_id: this.assessmentId,
        submission_id: this.submissionId,
        question_id: this.questionId,
        videoURL: playerState.player.getVideoUrl(),
        duration: playerState.player.getDuration(),
        currentTime: playerState.player.getCurrentTime(),
      });
    },
    onVideoEnded(playerState) {
      playerState.currentState = STATES.ENDED;
      playerState.currentTime = playerState.player.getCurrentTime();
    },
    onVideoPaused(playerState) {
      playerState.currentState = STATES.PAUSED;
      playerState.currentTime = playerState.player.getCurrentTime();
    },
    onVideoResumed(playerState) {
      playerState.currentState = STATES.RESUMED;
      playerState.currentTime = playerState.player.getCurrentTime();
    },
    onVideoRestarted(playerState) {
      playerState.currentState = STATES.RESTARTED;
      playerState.currentTime = playerState.player.getCurrentTime();
    },
    onVideoJumpedTo(playerState) {
      playerState.currentState = STATES.JUMPED_TO;
      playerState.currentTime = playerState.player.getCurrentTime();
    },
    onChangedResolution(playerState) {
      // TODO: map hd720 -> 720?
      // TODO: map hd1080 -> 1080?
      // TODO: map large -> 480?
      // TODO: map medium -> 360?
      playerState.currentState = STATES.CHANGED_RESOLUTION;
      playerState.currentTime = playerState.player.getCurrentTime();
    },
    onChangedSpeed(playerState) {
      playerState.currentState = STATES.CHANGED_SPEED;
      playerState.currentTime = playerState.player.getCurrentTime();
    },
    onFullscreenChange(event) {
      const playerState = this.getPlayerStateByIframe(event.target);
      if (document.fullscreenElement) {
        playerState.currentState = STATES.ENTERED_FULLSCREEN;
        playerState.currentTime = playerState.player.getCurrentTime();
      } else {
        playerState.currentState = STATES.EXITED_FULLSCREEN;
        playerState.currentTime = playerState.player.getCurrentTime();
      }
    },
    onMuted(playerState) {
      playerState.currentState = STATES.MUTED;
      playerState.currentTime = playerState.player.getCurrentTime();
      playerState.muted = true;
    },
    onUnmuted(playerState) {
      playerState.currentState = STATES.UNMUTED;
      playerState.currentTime = playerState.player.getCurrentTime();
      playerState.muted = false;
    },
    pollApiInfo() {
      // Detect state changes by polling API information at some interval. For
      // now just checking if video is muted.
      for (const playerState of this.playerStates) {
        const muted = playerState.player.isMuted();
        if (muted && !playerState.muted) {
          this.onMuted(playerState);
        } else if (!muted && playerState.muted) {
          this.onUnmuted(playerState);
        }
      }
    },
  },
  mounted() {
    this.$nextTick(() => {
      if (this.$el.querySelectorAll("iframe[data-youtube-id]").length > 0) {
        this.getYT({ callback: this.youtubeIframeAPIInit });
      }
    });
  },
  destroyed() {
    const allYoutubeIframes = this.$el.querySelectorAll(
      "iframe[data-youtube-id]"
    );
    // Remove fullscreenchange listers
    for (const iframe of allYoutubeIframes) {
      iframe.removeEventListener("fullscreenchange", this.onFullscreenChange);
    }
    // clear API polling interval
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  },
};
</script>

<style></style>
