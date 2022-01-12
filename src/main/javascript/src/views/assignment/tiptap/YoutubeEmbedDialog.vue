<template>
  <v-dialog :value="true" max-width="500px">
    <v-card>
      <v-card-title>
        <span class="headline">
          Youtube Embed
        </span>

        <v-spacer />

        <v-btn icon @click="close">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </v-card-title>

      <v-card-text>
        <v-textarea
          v-model="embedCode"
          hint="Paste the Youtube embed code above"
          placeholder="Youtube embed code"
        ></v-textarea>
      </v-card-text>

      <v-card-actions>
        <v-btn text @click="close">
          Close
        </v-btn>

        <v-btn text @click="add" :disabled="isDisabled">
          Add
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
export default {
  props: ["nativeExtensionName", "context", "editor"],
  data() {
    return {
      embedCode: null,
    };
  },
  computed: {
    iframe() {
      const parser = new DOMParser();
      const doc = parser.parseFromString(this.embedCode, "text/html");
      return doc.querySelector("iframe");
    },
    height() {
      if (this.iframe && this.iframe.height && parseInt(this.iframe.height)) {
        const height = parseInt(this.iframe.height);
        return height;
      } else {
        // Return undefined so that default value will be used
        return undefined;
      }
    },
    width() {
      if (this.iframe && this.iframe.width && parseInt(this.iframe.width)) {
        const width = parseInt(this.iframe.width);
        return width;
      } else {
        // Return undefined so that default value will be used
        return undefined;
      }
    },
    youtubeID() {
      // Supports pasting in the iframe embed code, or the short url
      const url = this.iframe ? this.iframe.src : this.embedCode;
      const youtubeID = url ? this.youtubeParser(url) : null;
      return youtubeID ? youtubeID : null;
    },
    isDisabled() {
      return !this.youtubeID;
    },
  },
  methods: {
    youtubeParser(url) {
      const regExp = /^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?))\??v?=?([^#&?]*).*/;
      const match = url.match(regExp);
      return match && match[7].length === 11 ? match[7] : false;
    },
    close() {
      this.$destroy();
      this.$el.parentNode.removeChild(this.$el);
    },
    add() {
      const youtubeID = this.youtubeID;
      const height = this.height;
      const width = this.width;
      if (youtubeID) {
        this.context.commands[this.nativeExtensionName]({
          youtubeID,
          height,
          width,
        });
      }
      this.close();
      this.editor.focus();
    },
  },
};
</script>

<style></style>
