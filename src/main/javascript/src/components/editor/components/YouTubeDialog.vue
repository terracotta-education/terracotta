<template>
  <v-dialog
    :value="dialog"
    max-width="500px"
  >
    <v-card>
      <v-card-title>
        <span
          class="headline"
        >
          Youtube Embed
        </span>
        <v-spacer />
        <v-btn
          @click="close"
          icon
        >
          <v-icon>
            mdi-close
          </v-icon>
        </v-btn>
      </v-card-title>
      <v-card-text>
        <v-textarea
          v-model="embedCode"
          hint="Paste the Youtube embed code above"
          placeholder="Youtube embed code"
          class="input-embed-code"
        ></v-textarea>
      </v-card-text>
      <v-card-actions>
        <v-btn
          @click="close"
          text
        >
          Close
        </v-btn>
        <v-btn
          :disabled="isDisabled"
          @click="add"
          text
        >
          Add
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import { parseIframeEmbed, youtubeParser } from "../util/YouTubeUtils";

export default {
  props: {
    editor: {
      type: Object,
      required: true,
    }
  },
  data() {
    return {
      dialog: false,
      embedCode: null,
      resolve: null
    }
  },
  computed: {
    iframe() {
      return parseIframeEmbed(this.embedCode);
    },
    height() {
      if (this.iframe && this.iframe.height && parseInt(this.iframe.height)) {
        return parseInt(this.iframe.height);
      } else {
        return 315;
      }
    },
    width() {
      if (this.iframe && this.iframe.width && parseInt(this.iframe.width)) {
        return parseInt(this.iframe.width);
      } else {
        return 560;
      }
    },
    youtubeId() {
      // Supports pasting in the iframe embed code, or the short url
      const url = this.iframe ? this.iframe.src : this.embedCode;
      const youtubeId = url ? youtubeParser(url) : null;

      return youtubeId ? youtubeId : null;
    },
    isDisabled() {
      return !this.embedCode;
    }
  },
  methods: {
    openDialog() {
      this.dialog = true;
      return new Promise(
        (resolve) => {
          this.resolve = resolve;
        }
      );
    },
    add() {
      const src = this.youtubeId ? "https://youtu.be/" + this.youtubeId : this.embedCode;
      const height = this.height;
      const width = this.width;
      this.resolve(
        {
          src,
          height,
          width,
        }
      );
      this.dialog = false;
    },
    close() {
      this.dialog = false;
    }
  }
}
</script>

<style scoped>
.input-embed-code {
  & .v-text-field__slot > label {
    left: 0px !important;
    right: auto !important;
  }
}
</style>
