<template>
  <div class="my-0 py-2">
    <h4
     class="my-0"
    >
      Scheduler
    </h4>

    <p class="text-medium-emphasis pb-0">
      {{ label }}
    </p>

    <v-row>
      <v-col cols="6">
        <DateTimePicker
          id="message-send-at"
          name="message-send-at"
          :model-value="send"
          :disabled="readOnly"
          aria-label="Send message date time picker"
          @update:model-value="processSendAt"
        />
      </v-col>
    </v-row>

    <v-row class="mx-0 my-0 pl-3">
      <span class="date-format-hint">
        MM/DD/YYYY HH:MM
      </span>
    </v-row>
  </div>
</template>

<script setup>
import {
  ref,
  watch
} from "vue";

import { validations } from "@/helpers/messaging/validation.js";
import DateTimePicker from "@/components/picker/DateTimePicker.vue";

defineOptions({
  name: "MessageScheduler"
});

const props = defineProps({
  sendAt: {
    type: String,
    default: null
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ""
  },
  validatedErrors: {
    type: Object,
    default: null
  }
});

const emit = defineEmits([
  "updated"
]);

const send = ref(props.sendAt);
const validationErrors = ref(null);

watch(
  () => props.sendAt,
  value => {
    send.value = value;
  }
);

watch(
  () => props.validatedErrors,
  value => {
    validationErrors.value =
      value || validations.container.sendAt;
  },
  {
    deep: true,
    immediate: true
  }
);

const processSendAt = date => {
  send.value = date;
  emit("updated", send.value);
};
</script>

<style scoped>
#message-send-at.datetime-input {
  margin-left: 0;
  padding: 16px;
}

.date-format-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.6);
}
</style>
