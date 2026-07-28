<template>
  <v-dialog
    v-model="dialog"
    max-width="500px"
  >
    <v-card>
      <v-card-title>
        <span class="text-title-large">
          Link URL
        </span>

        <v-spacer />

        <v-btn
          icon="mdi-close"
          variant="text"
          @click="close"
        />
      </v-card-title>

      <v-card-text>
        <v-text-field
          v-model="url"
          class="input-url"
          label="URL"
          variant="outlined"
        />
      </v-card-text>

      <v-card-actions>
        <v-btn
          variant="text"
          @click="close"
        >
          CLOSE
        </v-btn>

        <v-btn
          :disabled="isDisabled"
          variant="text"
          @click="apply"
        >
          APPLY
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted
} from "vue";

defineOptions({
  name: "LinkDialog"
});

const props = defineProps({
  editor: {
    type: Object,
    required: true
  },
  href: {
    type: String,
    default: ""
  }
});

const emit = defineEmits([
  "submit",
  "close"
]);

const dialog = ref(true);
const url = ref("");

const isDisabled = computed(() => {
  return !url.value;
});

watch(
  () => props.href,
  value => {
    url.value = value || "";
  }
);

watch(dialog, isOpen => {
  if (!isOpen) {
    emit("close");
  }
});

const apply = () => {
  emit("submit", url.value);
  dialog.value = false;
};

const close = () => {
  emit("close");
  dialog.value = false;
};

onMounted(() => {
  url.value = props.href || "";
});
</script>

<style scoped>
.input-url :deep(.v-field-label) {
  left: 0 !important;
  right: auto !important;
}
</style>
