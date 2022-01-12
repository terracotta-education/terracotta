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
    youtubeID() {
      const parser = new DOMParser();
      const doc = parser.parseFromString(this.embedCode, "text/html");
      const iframe = doc.querySelector("iframe");
      // Supports pasting in the iframe embed code, or the short url
      const url = iframe ? iframe.src : this.embedCode;
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
      if (youtubeID) {
        this.context.commands[this.nativeExtensionName]({
          youtubeID,
        });
      }
      this.close();
      this.editor.focus();
    },
  },
};
</script>

<style></style>
