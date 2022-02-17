<template>
  <div>
    <slot />
  </div>
</template>

<script>
import { mapActions } from "vuex";

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
    ...mapActions("mediaevents", [
      "getYT",
      "videoStarted",
      "videoEnded",
      "videoPaused",
      "videoResumed",
      "videoRestarted",
      "videoJumpedTo",
      "videoChangedResolution",
      "videoChangedSpeed",
      "videoEnteredFullScreen",
      "videoExitedFullScreen",
      "videoMuted",
      "videoUnmuted",
    ]),
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
              state.originalVideoUrl = state.player.getVideoUrl();
            },
            onStateChange: (event) => {
              const state = this.getPlayerState(event.target);
              if (event.data === YT.PlayerState.PLAYING && !state.started) {
                state.started = true;
                this.onVideoStarted(state);
              } else if (event.data === YT.PlayerState.ENDED) {
                this.onVideoEnded(state);
              } else if (event.data === YT.PlayerState.PAUSED) {
                this.onVideoPaused(state);
              } else if (
                event.data === YT.PlayerState.PLAYING &&
                state.started
              ) {
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
          currentTime: -1,
          muted: false,
          originalVideoUrl: null,
        });
      }
    },
    sendVideoEvent(storeAction, {player, originalVideoUrl}) {
      this[storeAction]({
        experiment_id: this.experimentId,
        condition_id: this.conditionId,
        treatment_id: this.treatmentId,
        assessment_id: this.assessmentId,
        submission_id: this.submissionId,
        question_id: this.questionId,
        originalVideoUrl: originalVideoUrl,
        videoURL: player.getVideoUrl(),
        duration: player.getDuration(),
        currentTime: player.getCurrentTime(),
      });
    },
    onVideoStarted(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoStarted", playerState);
    },
    onVideoEnded(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoEnded", playerState);
    },
    onVideoPaused(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoPaused", playerState);
    },
    onVideoResumed(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoResumed", playerState);
    },
    onVideoRestarted(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoRestarted", playerState);
    },
    onVideoJumpedTo(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoJumpedTo", playerState);
    },
    onChangedResolution(playerState) {
      // TODO: map hd720 -> 720?
      // TODO: map hd1080 -> 1080?
      // TODO: map large -> 480?
      // TODO: map medium -> 360?
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoChangedResolution", playerState);
    },
    onChangedSpeed(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      this.sendVideoEvent("videoChangedSpeed", playerState);
    },
    onFullscreenChange(event) {
      const playerState = this.getPlayerStateByIframe(event.target);
      if (document.fullscreenElement) {
        playerState.currentTime = playerState.player.getCurrentTime();
        this.sendVideoEvent("videoEnteredFullScreen", playerState);
      } else {
        playerState.currentTime = playerState.player.getCurrentTime();
        this.sendVideoEvent("videoExitedFullScreen", playerState);
      }
    },
    onMuted(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      playerState.muted = true;
      this.sendVideoEvent("videoMuted", playerState);
    },
    onUnmuted(playerState) {
      playerState.currentTime = playerState.player.getCurrentTime();
      playerState.muted = false;
      this.sendVideoEvent("videoUnmuted", playerState);
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
    // Remove fullscreenchange listeners
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
