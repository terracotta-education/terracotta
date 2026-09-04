<template>
  <v-btn-toggle
    v-model="toggled"
    :key="nonActivatableItemKey"
  >
    <ToolTip
      :icon="icon"
      :content="title"
      :ref="tooltipRef"
      alignment="top"
      activator-type="button"
      activator-class="item-button"
      size="small"
      @clicked="handleAction"
      @is-opened="emit('is-hovered', tooltipRef)"
    />
  </v-btn-toggle>
</template>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

import ToolTip from "@/components/ToolTip.vue";

defineOptions({
  name: "ToolbarItem"
});

const props = defineProps({
  editor: {
    type: Object,
    required: true
  },
  icon: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  action: {
    type: String,
    required: true
  },
  activatable: {
    type: Boolean,
    default: false
  },
  attributes: {
    type: Object,
    default: null
  },
  activate: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "clicked",
  "is-hovered"
]);

const buttonIndex = 0;
const toggled = ref(null);
const nonActivatableItemKey = ref(0);

const tooltipRef = computed(() => {
  return `tooltip-item-${props.icon}`;
});

const isEditorActive = () => {
  if (!props.editor) {
    return false;
  }

  if (props.attributes) {
    return props.editor.isActive(
      props.action,
      props.attributes
    );
  }

  return props.editor.isActive(props.action);
};

watch(
  () => props.editor,
  () => {
    if (!props.activatable) {
      toggled.value = null;
      return;
    }

    if (!isEditorActive()) {
      toggled.value = null;
    }
  },
  {
    deep: true
  }
);

watch(
  () => props.activate,
  isActive => {
    if (!props.activatable) {
      toggled.value = null;
      return;
    }

    if (!isActive && isEditorActive()) {
      return;
    }

    toggled.value =
      isActive && isEditorActive()
        ? buttonIndex
        : null;
  }
);

const reloadNonActivatableItem = () => {
  if (!props.activatable) {
    nonActivatableItemKey.value++;
  }
};

const handleAction = () => {
  emit(
    "clicked",
    props.action,
    props.attributes
  );

  reloadNonActivatableItem();
};
</script>

<style scoped>
.v-btn-toggle:deep(*) {
  background-color: transparent !important;
}

.v-btn-toggle:deep(.item-button) {
  margin: 2px 6px;
  border: none !important;
  border-radius: 50% !important;
}

.v-btn-toggle:deep(.item-button.v-btn--icon:first-child) {
  margin-left: 6px !important;
}

.v-btn-toggle:deep(.item-button .v-icon) {
  color: rgba(0, 0, 0, 0.7) !important;
}
</style>
