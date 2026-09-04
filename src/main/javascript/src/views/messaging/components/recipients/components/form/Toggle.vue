<template>
  <v-btn-toggle
    v-model="newSelection"
    density="compact"
    color="primary"
    mandatory
  >
    <v-btn
      :value="leftOption"
      :disabled="readOnly"
    >
      {{ leftOption }}
    </v-btn>

    <v-btn
      :value="rightOption"
      :disabled="readOnly"
    >
      {{ rightOption }}
    </v-btn>
  </v-btn-toggle>
</template>

<style scoped>
/* Vuetify 2's v-btn-toggle gives every button a thin rgba(0,0,0,.12) border by
   default; Vuetify 3's doesn't, so the unselected button (white background, no
   shadow) was invisible against a white card. */
.v-btn {
  border: thin solid rgba(0, 0, 0, 0.12);
}
</style>

<script setup>
import {
  ref,
  computed,
  watch
} from "vue";

defineOptions({
  name: "RecipientToggle"
});

const props = defineProps({
  selectedOption: {
    type: String,
    default: null
  },
  options: {
    type: Array,
    required: true
  },
  readOnly: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "update"
]);

const newSelection = ref(null);

const leftOption = computed(() => {
  return props.options[0];
});

const rightOption = computed(() => {
  return props.options[1];
});

watch(
  () => props.selectedOption,
  value => {
    newSelection.value = value;
  },
  {
    immediate: true
  }
);

watch(newSelection, value => {
  emit("update", value);
});
</script>

