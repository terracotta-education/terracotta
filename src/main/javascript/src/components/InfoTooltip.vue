<template>
  <v-tooltip
    :top="getLocationTop"
    :bottom="getLocationBottom"
    :right="getLocationRight"
    :left="getLocationLeft"
  >
    <template
      #activator="{ on }"
    >
      <span
        v-on="on"
      >
        <v-icon
          v-if="getActivatorType === 'icon'"
          :style="getIconStyle"
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
    "linkClass",
    "iconStyle", // {css styles}
    "location", // [top, bottom, right, left] (default to top)
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
    },
    getIconStyle() {
      return this.iconStyle || "";
    },
    getLocation() {
      return this.location || "top";
    },
    getLocationTop() {
      return this.getLocation === "top";
    },
    getLocationBottom() {
      return this.getLocation === "bottom";
    },
    getLocationRight() {
      return this.getLocation === "right";
    },
    getLocationLeft() {
      return this.getLocation === "left";
    }
  }
}
</script>

<style scoped>
h3 {
  font-weight: bold;
}
</style>
