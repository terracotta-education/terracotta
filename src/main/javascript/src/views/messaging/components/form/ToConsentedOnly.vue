<template>
  <div>
    <v-switch
      v-model="selection"
      :disabled="readOnly || !enabledByConsent"
      :ripple="false"
      label="Send messages to consented individuals only"
      inset
    />
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

defineOptions({
  name: "ToConsentedOnly"
});

const props = defineProps({
  selected: {
    type: Boolean,
    default: false
  },
  experiment: {
    type: Object,
    required: true
  },
  readOnly: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "updated"
]);

const selection = ref(false);

const enabledByConsent = computed(() => {
  return props.experiment?.participationType === "CONSENT";
});

watch(
  () => props.selected,
  value => {
    selection.value = enabledByConsent.value
      ? value
      : false;
  },
  {
    immediate: true
  }
);

watch(selection, value => {
  emit("updated", value);
});

watch(enabledByConsent, enabled => {
  if (!enabled) {
    selection.value = false;
  }
});
</script>
