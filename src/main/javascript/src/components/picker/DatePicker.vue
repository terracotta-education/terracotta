<template>
  <v-dialog
    ref="sendDateDialog"
    v-model="displayPicker"
    :return-value.sync="date"
    :width="getWidth"
  >
    <template v-slot:activator="{ on, attrs }">
      <v-text-field
        v-model="displayedDate"
        :label="getLabel"
        :hide-details="validatedErrors === null"
        :error-messages="validationErrors"
        :disabled="readOnly"
        :readonly="readOnly"
        v-bind="attrs"
        v-on="on"
        append-icon="mdi-calendar-blank-outline"
        outlined
      />
      <v-row
        class="mx-0 my-0 pl-3"
      >
        <span
          class="date-format-hint"
        >
          MM/DD/YYYY
        </span>
      </v-row>
    </template>
    <v-date-picker
      v-if="!readOnly && displayPicker"
      v-model="date"
      :min="new Date().toISOString()"
      scrollable
    >
      <v-spacer />
      <v-btn
        text
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
    </v-date-picker>
  </v-dialog>
</template>

<script>
import { validations } from "@/helpers/messaging/validation.js";
import moment from "moment";

export default {
  name: "DatePicker",
  props: {
    date: {
      type: String,
      required: false
    },
    label: {
      type: String,
      default: "Date"
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
        this.validationErrors = newValidatedErrors || validations.container.sendAt.date;

        if (this.date) {
          this.validationErrors = null;
        }
      },
      immediate: true
    }
  },
  computed: {
    getLabel() {
      return this.label || "Date";
    },
    getWidth() {
      return this.width || "290px";
    },
    displayedDate() {
      if (!this.date) {
        return "Please select a date.";
      }

      return moment(this.date).format("MM/DD/YYYY");
    }
  },
  methods: {
    handleOk() {
      this.$refs.sendDateDialog.save(this.date);
      this.$emit("updated", this.date);
    },
    handleCancel() {
      this.displayPicker = false;
    }
  }
}
</script>

<style scoped>
.date-format-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.6);
}
</style>
