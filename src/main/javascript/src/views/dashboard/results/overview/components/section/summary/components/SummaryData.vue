<template>
<v-col>
  <v-row
    class="header-row"
  >
    <h4>{{ headerTitle }}</h4>
    <v-img
      v-if="headerIcon"
      :style="dynamicStyles"
      :src="headerIcon"
      class="header-icon"
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
      <tool-tip
        v-if="displayTooltip"
        :header="headerTitle"
        :content="tooltip"
        :activatorType="tooltipActivator.type"
        :icon="tooltipActivator.text"
        :aria-label="`${headerTitle} tooltip`"
      />
    </span>
  </v-row>
</v-col>
</template>

<script>
import ToolTip from "@/components/ToolTip.vue";

export default {
  name: "SummaryCount",
  components: {
    ToolTip
  },
  props: {
    title: {
      type: String,
      required: false
    },
    value: {
      type: [Number, String],
      required: false
    },
    message: {
      type: String,
      required: false
    },
    icon: {
      type: String,
      required: false
    },
    iconBgColor: {
      type: String,
      required: false
    },
    valueFontSize: {
      type: String,
      required: false
    },
    showTooltip: {
      type: Boolean,
      required: false
    }
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
  > span.summary-count {
    max-width: fit-content;
    font-size: 2em;
    font-weight: bold;
  }
}
</style>
