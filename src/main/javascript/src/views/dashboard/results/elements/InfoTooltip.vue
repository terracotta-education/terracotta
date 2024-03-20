<template>
  <v-tooltip
    top
  >
    <template
      #activator="{ on }"
    >
      <span
        v-on="on"
      >
        <v-icon
          v-if="getActivatorType === 'icon'"
        >
          {{  getActivatorText }}
        </v-icon>
        <a
          v-if="getActivatorType === 'link'"
          :class="getLinkClass"
        >
          {{  getActivatorText }}
        </a>
      </span>
    </template>
    <h3
      v-if="hasHeader"
    >
      {{ getHeader }}
    </h3>
    <span>{{ getMessage }}</span>
  </v-tooltip>
</template>

<script>
export default {
  name: "InfoTooltip",
  props: [
    "header",
    "message",
    "activator", // {type, text}
    "linkClass"
  ],
  computed: {
    hasHeader() {
      return this.header !== null;
    },
    getHeader() {
      return this.header || "N/A";
    },
    getMessage() {
      return this.message || "N/A"
    },
    getActivator() {
      return this.activator || {};
    },
    getActivatorType() {
      return this.getActivator.type || null;
    },
    getActivatorText() {
      return this.getActivator.text || null;
    },
    getLinkClass() {
      return this.linkClass || "";
    }
  }
}
</script>

<style scoped>
h3 {
  font-weight: bold;
}
</style>
