<template>
<v-tooltip
  v-model="showToolTip"
  :contained="isContained"
  :location="location"
  transition=""
  class="tool-tip"
  content-class="tool-tip-content"
>
  <template
    v-slot:activator="{ props }"
  >
    <v-btn
      v-if="isButton"
      v-bind="props"
      :href="url"
      :aria-label="ariaLabel"
      :icon="showIcon"
      :class="activatorClass"
      :size="size"
      @mouseenter="onActivatorEnter"
      @focus="onActivatorEnter"
      @mouseleave="onActivatorLeave(1000)"
      @blur="onActivatorLeave(0)"
      @click="$emit('clicked')"
      tabindex="0"
      target="_blank"
      variant="text"
      density="compact"
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
      v-bind="props"
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
      v-bind="props"
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
      v-bind="props"
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

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted,
  onBeforeUnmount
} from "vue";

defineOptions({
  name: "ToolTip"
});

const props = defineProps({
  header: String,
  content: {
    type: String,
    required: true
  },
  activatorType: {
    type: String,
    default: "button"
  },
  activatorContent: String,
  activatorClass: String,
  activatorIconClass: String,
  url: String,
  alignment: {
    type: String,
    default: "bottom"
  },
  ariaLabel: {
    type: String,
    default: "Tooltip activated"
  },
  icon: String,
  iconLabel: {
    type: String,
    default: "Tooltip icon"
  },
  iconColor: {
    type: String,
    default: "primary"
  },
  iconStyle: String,
  contained: {
    type: Boolean,
    default: false
  },
  attach: {
    type: [String, Boolean],
    default: false
  },
  size: String
});

const emit = defineEmits([
  "clicked",
  "is-opened"
]);

const tooltipId = Symbol();

const showToolTip = ref(false);
const inActivator = ref(false);
const inContent = ref(false);
const timeoutId = ref(null);

const showHeader = computed(() => !!props.header);
const showIcon = computed(() => !!props.icon);
const showActivatorContent = computed(() => !!props.activatorContent);

const location = computed(() => props.alignment);
const size = computed(() => props.size);

const isButton = computed(() => props.activatorType === "button");
const isLink = computed(() => props.activatorType === "link");
const isParagraph = computed(() => props.activatorType === "paragraph");
const isIcon = computed(() => props.activatorType === "icon");

const isContained = computed(() => props.contained);

watch(showToolTip, newVal => {
  if (newVal) {
    emit("is-opened");
  }
});

const open = () => {
  showToolTip.value = true;
};

const close = () => {
  showToolTip.value = false;
  inContent.value = false;
  inActivator.value = false;
};

const clear = () => {
  if (timeoutId.value) {
    clearTimeout(timeoutId.value);
    timeoutId.value = null;
  }
};

const onActivatorEnter = () => {
  clear();
  inActivator.value = true;
  open();
  window.dispatchEvent(new CustomEvent("terracotta:tooltip-opened", { detail: tooltipId }));
};

const onActivatorLeave = delay => {
  open();
  inActivator.value = false;

  timeoutId.value = setTimeout(() => {
    if (!inContent.value) {
      close();
    }
  }, delay);
};

const onContentEnter = () => {
  clear();
  open();
  inActivator.value = false;
  inContent.value = true;
};

const onContentLeave = () => {
  inContent.value = false;

  timeoutId.value = setTimeout(() => {
    if (!inActivator.value) {
      close();
    }
  }, 500);
};

const handleKeyPress = event => {
  if (event.key === "Escape" || event.key === "Esc") {
    close();
  }
};

const handleOtherTooltipOpened = event => {
  if (event.detail !== tooltipId) {
    close();
  }
};

onMounted(() => {
  window.addEventListener("keydown", handleKeyPress);
  window.addEventListener("terracotta:tooltip-opened", handleOtherTooltipOpened);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleKeyPress);
  window.removeEventListener("terracotta:tooltip-opened", handleOtherTooltipOpened);
  clear();
});
</script>

<style lang="scss">
/* Unscoped — overlay content is teleported to <body>, outside the component DOM */
.v-overlay__content.tool-tip-content {
  max-width: 400px !important;
  opacity: 1.0 !important;
  background-color: rgba(55, 61, 63, 1.0) !important;
  color: #fff !important;
  pointer-events: auto !important;
  padding: 0;

  .tool-tip-content-body {
    padding: 5px 16px;
    text-align: left;

    a {
      color: #afdcff;
    }
  }
}
</style>

<style lang="scss" scoped>
/* Scoped — activator element lives inside the component DOM */
.has-tooltip {
  text-decoration-style: dashed;
  text-decoration-line: underline;
  color: map.get($blue, "base");
}
</style>
