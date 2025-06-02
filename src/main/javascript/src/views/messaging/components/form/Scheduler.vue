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
      <date-picker
        :date="send.date"
        :readOnly="readOnly"
        :validatedErrors="validationErrors.date"
        label="Date"
        @updated="processSendDate"
      />
    </div>
    <div
      class="col-6"
    >
      <time-picker
        :time="send.time"
        :readOnly="readOnly"
        :validatedErrors="validationErrors.time"
        label="Time"
        @updated="processSendTime"
      />
    </div>
  </v-row>
</div>
</template>

<script>
import { validations } from "@/helpers/messaging/validation.js";
import moment from "moment";
import DatePicker from "@/components/picker/DatePicker";
import TimePicker from "@/components/picker/TimePicker";

export default {
  components: {
    DatePicker,
    TimePicker
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
    send: {
      date: null,
      time: null
    },
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
    processSendDate(date) {
      this.send.date = date;
      this.processSendAt();
    },
    processSendTime(time) {
      this.send.time = time;
      this.processSendAt();
    },
    processSendAt() {
      this.$emit("updated", this.send.date && this.send.time ? moment(`${this.send.date}T${this.send.time}`).valueOf() : null);
    },
  },
  mounted() {
    if (!this.sendAt) {
      return;
    }

    this.send = {
      date: moment(this.sendAt).format("YYYY-MM-DD"),
      time: moment(this.sendAt).format("HH:mm:ss")
    }
  }
}
</script>
