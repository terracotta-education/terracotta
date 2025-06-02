<template>
  <v-dialog
    ref="sendTimeDialog"
    v-model="displayPicker"
    :return-value.sync="time"
    :width="getWidth"
  >
    <template v-slot:activator="{ on, attrs }">
      <v-text-field
        v-model="displayedTime"
        :label="getLabel"
        :hide-details="validatedErrors === null"
        :error-messages="validationErrors"
        :disabled="readOnly"
        :readonly="readOnly"
        v-bind="attrs"
        v-on="on"
        append-icon="mdi-clock-time-four-outline"
        outlined
      />
    </template>
    <v-time-picker
      v-if="!readOnly && displayPicker"
      v-model="time"
      full-width
      scrollable
    >
      <v-spacer></v-spacer>
      <v-btn
        text
        color="primary"
        @click="handleCancel()"
      >
        Cancel
      </v-btn>
      <v-btn
        text
        color="primary"
        @click="handleOk()"
      >
        OK
      </v-btn>
    </v-time-picker>
  </v-dialog>
</template>

<script>
import { validations } from "@/helpers/messaging/validation.js";
import moment from 'moment';

export default {
  name: "TimePicker",
  props: {
    time: {
      type: String,
      required: false
    },
    label: {
      type: String,
      default: "Time"
    },
    width: {
      type: String,
      required: false
    },
    readOnly: {
      type: Boolean,
      default: false
    },
    validatedErrors: {
      type: Object,
      default: null
    }
  },
  data: () => ({
    displayPicker: false,
    validationErrors: null
  }),
  watch: {
    validatedErrors: {
      handler(newValidatedErrors) {
        this.validationErrors = newValidatedErrors || validations.container.sendAt.time;

        if (this.time) {
          this.validationErrors = null;
        }
      },
      immediate: true
    }
  },
  computed: {
    getLabel() {
      return this.label || "Time";
    },
    getWidth() {
      return this.width || "290px";
    },
    displayedTime() {
      if (!this.time) {
        return "Please select a time.";
      }

      return moment(this.time, "HH:mm").format("h:mm A")
    }
  },
  methods: {
    handleOk() {
      this.$refs.sendTimeDialog.save(this.time);
      this.$emit("updated", this.time);
    },
    handleCancel() {
      this.displayPicker = false;
    }
  }
}
</script>
