<template>
<v-tooltip
  v-model="showToolTip"
  :contained="isContained"
  :bottom="isBottom"
  :left="isLeft"
  :right="isRight"
  :top="isTop"
  :attach="attach"
  transition=""
  class="tool-tip"
  content-class="tool-tip-content"
>
  <template
    v-slot:activator="{ attrs }"
  >
    <v-btn
      v-if="isButton"
      v-bind="attrs"
      :href="url"
      :aria-label="ariaLabel"
      :icon="showIcon"
      :class="activatorClass"
      :small="isSmall"
      :large="isLarge"
      @mouseenter="onActivatorEnter"
      @focus="onActivatorEnter"
      @mouseleave="onActivatorLeave(1000)"
      @blur="onActivatorLeave(0)"
      @click="$emit('clicked')"
      tabindex="0"
      target="_blank"
    >
      <v-icon
        v-if="showIcon"
        :alt="iconLabel"
        :color="iconColor"
        :class="activatorIconClass"
        aria-hidden="false"
      >
        {{ icon }}
      </v-icon>
      <span
        v-if="showActivatorContent"
      >
        {{ activatorContent }}
      </span>
    </v-btn>
    <a
      v-else-if="isLink"
      v-bind="{role: attrs.role, 'aria-haspopup': attrs.haspopup}"
      @mouseenter="onActivatorEnter"
      @focus="onActivatorEnter"
      @mouseleave="onActivatorLeave(1000)"
      @blur="onActivatorLeave(0)"
      :aria-label="ariaLabel"
      :class="activatorClass"
      target="_blank"
      tabindex="0"
    >
      <v-icon
        v-if="showIcon"
        :alt="iconLabel"
        :color="iconColor"
        tabindex="0"
      >
        {{ icon }}
      </v-icon>
      {{ activatorContent }}
    </a>
    <p
      v-else-if="isParagraph"
      v-bind="attrs"
      @mouseenter="onActivatorEnter"
      @focus="onActivatorEnter"
      @mouseleave="onActivatorLeave(1000)"
      @blur="onActivatorLeave(0)"
      :aria-label="ariaLabel"
      :class="activatorClass"
      class="has-tooltip"
      tabindex="0"
    >
      <v-icon
        v-if="showIcon"
        :alt="iconLabel"
        :color="iconColor"
      >
        {{ icon }}
      </v-icon>
      {{ activatorContent }}
    </p>
    <v-icon
      v-else-if="isIcon"
      v-bind="{role: attrs.role, 'aria-haspopup': attrs.haspopup}"
      @mouseenter="onActivatorEnter"
      @focus="onActivatorEnter"
      @mouseleave="onActivatorLeave(1000)"
      @blur="onActivatorLeave(0)"
      :alt="iconLabel"
      :color="iconColor"
      :class="activatorClass"
      tabindex="0"
      aria-hidden="false"
    >
      {{ icon }}
    </v-icon>
  </template>
  <div
    @mouseenter="onContentEnter"
    @focus="onContentEnter"
    @mouseleave="onContentLeave(0)"
    @blur="onContentLeave(0)"
    class="tool-tip-content-body"
  >
    <strong
      v-if="showHeader"
    >
      {{ header }}
    </strong>
    <br
      v-if="showHeader"
    />
    {{ content }}
  </div>
</v-tooltip>
</template>

<script>
import { mapGetters } from "vuex";

export default {
  name: "ToolTip",
  props: {
    header: {
      type: String,
      required: false
    },
    content: {
      type: String,
      required: true
    },
    activatorType: {
      type: String,
      required: false,
      default: "button"
    },
    activatorContent: {
      type: String,
      required: false
    },
    activatorClass: {
      type: String,
      required: false
    },
    activatorIconClass: {
      type: String,
      required: false
    },
    url: {
      type: String,
      required: false
    },
    alignment: {
      type: String,
      required: false,
      default: "bottom"
    },
    ariaLabel: {
      type: String,
      required: false,
      default: "Tooltip activated"
    },
    icon: {
      type: String,
      required: false
    },
    iconLabel: {
      type: String,
      required: false,
      default: "Tooltip icon"
    },
    iconColor: {
      type: String,
      required: false,
      default: "primary"
    },
    iconStyle: {
      type: String,
      required: false
    },
    contained: {
      type: Boolean,
      required: false,
      default: false
    },
    attach: {
      type: [String, Boolean],
      required: false,
      default: false
    },
    size: {
      type: String,
      required: false
    }
  },
  data: () => ({
    showToolTip: false,
    inActivator: false,
    inContent: false,
    timeoutId: null
  }),
  watch: {
    showToolTip(newVal) {
      if (newVal) {
        this.$emit("is-opened");
      }
    }
  },
  computed: {
    ...mapGetters({
      configurations: "configuration/get"
    }),
    showHeader() {
      return !!this.header;
    },
    showIcon() {
      return !!this.icon;
    },
    showActivatorContent() {
      return !!this.activatorContent;
    },
    isBottom() {
      return this.alignment === "bottom";
    },
    isLeft() {
      return this.alignment === "left";
    },
    isRight() {
      return this.alignment === "right";
    },
    isTop() {
      return this.alignment === "top";
    },
    isButton() {
      return this.activatorType === "button";
    },
    isLink() {
      return this.activatorType === "link";
    },
    isParagraph() {
      return this.activatorType === "paragraph";
    },
    isIcon() {
      return this.activatorType === "icon";
    },
    isContained() {
      return this.contained;
    },
    isSmall() {
      return this.size === "small";
    },
    isLarge() {
      return this.size === "large";
    }
  },
  methods: {
    onActivatorEnter() {
      this.clear();
      this.inActivator = true;
      this.open();
    },
    onActivatorLeave(delay) {
      this.inActivator = false;
      this.timeoutId = setTimeout(() => {
        if (!this.inContent) {
          this.close();
        }
      }, delay);
    },
    onContentEnter() {
      this.clear();
      this.open();
      this.inActivator = false;
      this.inContent = true;
    },
    onContentLeave() {
      this.inContent = false;
      this.timeoutId = setTimeout(() => {
        if (!this.inActivator) {
          this.close();
        }
      }, 500);
    },
    open() {
      this.showToolTip = true;
    },
    close() {
      this.showToolTip = false;
      this.inContent = false;
      this.inActivator = false;
    },
    clear() {
      if (this.timeoutId) {
        clearTimeout(this.timeoutId);
        this.timeoutId = null;
      }
    },
    handleKeyPress(event) {
      switch (event.key) {
        case "Escape":
        case "Esc":
          this.close();
          break;
        default:
          break;
      }
    }
  },
  mounted() {
    window.addEventListener("keydown", this.handleKeyPress);
  },
  beforeDestroy() {
    window.removeEventListener("keydown", this.handleKeyPress);
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/variables";

.tool-tip-content {
  &.v-tooltip__content {
    max-width: 400px;
    opacity: 1.0 !important;
    background-color: rgba(55,61,63, 1.0) !important;
    pointer-events: auto !important;
    padding: 0;
    & .tool-tip-content-body {
      padding: 5px 16px;
      text-align: left;
      a {
        color: #afdcff;
      }
    }
  }
  & .has-tooltip {
    text-decoration-style: dashed;
    text-decoration-line: underline;
    color: map-get($blue, "base");
  }
}
</style>
