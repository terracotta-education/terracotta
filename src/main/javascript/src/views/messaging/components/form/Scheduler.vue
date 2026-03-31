<template>
<div
  class="mt-2 mb-5 py-2"
>
  <h4>Scheduler</h4>
  <p
    class="grey--text text--darken-2 pb-0"
  >
    {{ label }}
  </p>
  <v-row>
    <div
      class="col-6"
    >
      <date-time-picker
        :value="sendAt"
        @input="processSendAt"
        id="message-send-at"
        name="message-send-at"
        ariaLabel="Send message date time picker"
      />
    </div>
  </v-row>
  <v-row
    class="mx-0 my-0 pl-3"
  >
    <span
      class="date-format-hint"
    >
      MM/DD/YYYY HH:MM
    </span>
  </v-row>
</div>
</template>

<script>
import { validations } from "@/helpers/messaging/validation.js";
import DateTimePicker from "@/components/picker/DateTimePicker.vue";

export default {
  components: {
    DateTimePicker
  },
  props: {
    sendAt: {
      type: String,
      required: false
    },
    readOnly: {
      type: Boolean,
      default: false
    },
    label: {
      type: String
    },
    validatedErrors: {
      type: Object,
      default: null
    }
  },
  data: () => ({
    send: null,
    validationErrors: null
  }),
  watch: {
    validatedErrors: {
      handler(newValidatedErrors) {
        this.validationErrors = newValidatedErrors || validations.container.sendAt;
      },
      deep: true,
      immediate: true
    }
  },
  methods: {
    processSendAt(date) {
      this.send = date;
      this.$emit("updated", this.send);
    },
  },
  mounted() {
    this.send = this.sendAt;
  }
}
</script>

<style scoped>
#message-send-at {
  &.datetime-input {
    margin-left: 0;
    padding: 16px;
  }
}
.date-format-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.6);
}
</style>
