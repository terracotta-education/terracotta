<template>
  <div
    :id="id"
    :class="[
      classes,
      'datetime-input d-flex align-center justify-space-between'
    ]"
    tabindex="0"
    @click="open"
    @focus="open"
    @blur="close"
  >
    <FlatPickr
      ref="flatpickr"
      v-model="date"
      :config="config"
      :aria-label="ariaLabel"
      :class="classes"
      :name="name"
      :disabled="disabled"
      tabindex="0"
      @on-change="handleDateChange"
    />

    <v-icon>
      mdi-calendar-clock
    </v-icon>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onMounted,
  ref,
  watch
} from "vue";

import FlatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.min.css";

defineOptions({
  name: "DateTimePicker"
});

const props = defineProps({
  id: {
    type: String,
    default: null
  },
  name: {
    type: String,
    default: null
  },
  classes: {
    type: String,
    default: null
  },
  ariaLabel: {
    type: String,
    default: "Date and Time Picker"
  },
  modelValue: {
    type: String,
    default: null
  },
  min: {
    type: String,
    default: null
  },
  max: {
    type: String,
    default: null
  },
  enableDate: {
    type: Boolean,
    default: true
  },
  enableTime: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "update:modelValue"
]);

const flatpickr = ref(null);
const date = ref(props.modelValue);

const config = computed(() => {
  return {
    enableTime: props.enableTime,
    noCalendar: !props.enableDate,
    dateFormat: "Z",
    minDate: props.min,
    maxDate: props.max,
    altInput: true,
    altFormat: props.enableTime
      ? "m/d/Y h:iK"
      : "m/d/Y",
    ariaDateFormat: props.enableTime
      ? "M j, Y h:i K"
      : "M j, Y"
  };
});

watch(
  () => props.modelValue,
  value => {
    date.value = value;

    if (flatpickr.value?.fp) {
      flatpickr.value.fp.setDate(value, false);
    }
  }
);

watch(date, value => {
  emit("update:modelValue", value);
});

const setScopedAriaLabel = () => {
  const input =
    flatpickr.value?.fp?.altInput ||
    flatpickr.value?.fp?.input;

  if (!input) {
    return;
  }

  input.setAttribute(
    "aria-label",
    props.ariaLabel
  );
};

const open = () => {
  if (props.disabled) {
    return;
  }

  flatpickr.value?.fp?.open();
};

const close = () => {
  flatpickr.value?.fp?.close();
};

const handleDateChange = (_selectedDates, dateString) => {
  date.value = dateString;
};

onMounted(async () => {
  await nextTick();

  flatpickr.value?.fp?.setDate(date.value, true);
  setScopedAriaLabel();
});
</script>

<style scoped>
.datetime-input {
  color: rgba(0, 0, 0, 0.87);
  border: 1px solid #a0a0a0;
  border-radius: 4px;
  max-width: fit-content;
  padding: 8px;
  margin: 0 8px;
}
</style>

<style>
.datetime-input input {
  border: none !important;
  box-shadow: none !important;
  outline: none !important;
  background: transparent !important;
}
</style>
