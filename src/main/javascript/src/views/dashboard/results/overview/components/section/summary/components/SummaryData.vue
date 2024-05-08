<template>
  <v-col>
    <v-row
      class="header-row"
    >
      <h4>{{ headerTitle }}</h4>
      <v-img
        v-if="headerIcon"
        :style="dynamicStyles"
        class="header-icon"
        :src="headerIcon"
      >
      </v-img>
    </v-row>
    <v-row
      class="count-row pt-4"
    >
      <span
        :style="valueStyle"
        class="summary-count"
      >
        {{ count }}
        <InfoTooltip
          v-if="displayTooltip"
          :header="headerTitle"
          :message="tooltip"
          :activator="tooltipActivator"
        />
      </span>
    </v-row>
  </v-col>
</template>

<script>
import InfoTooltip from "@/components/InfoTooltip.vue";

export default {
  name: "SummaryCount",
  props: [
    "title",
    "value",
    "message",
    "icon",
    "iconBgColor",
    "valueFontSize",
    "showTooltip"
  ],
  components: {
    InfoTooltip
  },
  computed: {
    headerTitle() {
      return this.title || "N/A";
    },
    headerIcon() {
      return this.icon || null;
    },
    count() {
      return this.value || 0;
    },
    tooltip() {
      return this.message || "N/A";
    },
    displayTooltip() {
      return this.showTooltip || false;
    },
    tooltipActivator() {
      return {"type": "icon", "text": "mdi-information-outline"};
    },
    valueStyle() {
      return {
        fontSize: this.valueFontSize || "2em"
      }
    },
    dynamicStyles() {
      return {
        "--header-icon-bg-color": (this.iconBgColor || 'transparent')
      };
    }
  }
}
</script>

<style scoped>
div.header-row {
  justify-content: space-between;
  > h3,
  > .header-icon {
    max-width: fit-content;
    border-radius: 4px;
  }
  > h3 {
    padding-bottom: 0 !important;
    word-break: break-all;
  }
  > .header-icon {
    > .v-image__image {
      background-color: var(--header-icon-bg-color);
    }
  }
}
div.count-row {
  /*justify-content: center;*/
  > span.summary-count {
    max-width: fit-content;
    font-size: 2em;
    font-weight: bold;
  }
}
</style>
