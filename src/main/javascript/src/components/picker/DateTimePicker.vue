<template>
<div
  :id="id"
  :class="classes"
  @click="open"
  @focus="open"
  @blur="close"
  class="datetime-input d-flex align-items-center justify-content-between"
>
  <flat-pickr
    v-model="date"
    @on-change="handleDateChange"
    :config="config"
    :aria-label="ariaLabel"
    :class="classes"
    :name="name"
    ref="flatpickr"
    tabindex="0"
  ></flat-pickr>
  <v-icon>mdi-calendar-clock</v-icon>
</div>
</template>

<script>
import { addAttributesToElement } from "@/helpers/ui-utils.js";
import flatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.min.css";

export default {
  components: {
    flatPickr,
  },
  props: {
    id: {
      type: String
    },
    name: {
      type: String
    },
    classes: {
      type: String
    },
    ariaLabel: {
      type: String,
      default: "Date and Time Picker",
    },
    value: {
      type: String,
      default: null
    },
    min: {
      type: String
    },
    max: {
      type: String
    },
    enableDate: {
      type: Boolean,
      default: true
    },
    enableTime: {
      type: Boolean,
      default: true
    }
  },
  data: () => ({
    date: null
  }),
  watch: {
    date: {
      handler(newDate) {
        this.$emit("input", newDate);
      }
    }
  },
  computed: {
    config() {
      return {
        enableTime: true,
        dateFormat: "Z",
        minDate: this.min,
        maxDate: this.max,
        altInput: true,
        altFormat: "m/d/Y h:iK",
        ariaDateFormat: "M j, Y h:i K"
      }
    }
  },
  methods: {
    open() {
      this.$refs.flatpickr.fp.open();
    },
    close() {
      this.$refs.flatpickr.fp.close();
    },
    handleDateChange(_, dateStr) {
      this.date = dateStr;
    }
  },
  mounted() {
    this.date = this.value;
    this.$nextTick(() => {
      this.$refs.flatpickr.fp.setDate(this.date, true);
      addAttributesToElement(
        ".form-control.input",
        [{
          name: "aria-label",
          value: this.ariaLabel
        }]
      )
    });
  }
}
</script>

<style scoped>
.datetime-input {
  color: rgba(0, 0, 0, .87);
  border: 1px solid #a0a0a0;
  border-radius: 4px;
  max-width: fit-content;
  padding: 8px;
  margin: 0 8px;
}
</style>
