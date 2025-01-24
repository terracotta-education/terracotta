<template>
  <div>
    <h4>
      {{  messages.headerText }}
    </h4>
    <p
      class="grey--text text--darken-2 pb-0"
    >
      {{ messages.headerInstructionsText }}
    </p>
    <v-card
      outlined
      class="reveal-responses-card"
    >
      <v-card-title
        :class="{ 'blue lighten-5': allowStudentViewResponses }"
      >
        <v-checkbox
          v-model="allowStudentViewResponses"
          :label="messages.allowStudentViewResponsesLabel"
          @change="changeAllowStudentViewResponses"
          class="mt-0"
          hide-details
        />
      </v-card-title>
      <v-card-text
        v-if="allowStudentViewResponses"
        class="text--primary"
      >
        <div
          class="d-flex flex-wrap align-baseline mt-5"
        >
          <div>
            {{ messages.allowStudentViewResponsesDatesLabel }}
          </div>
          <custom-datetime-picker
            v-model="studentViewResponsesAfter"
            :datePickerProps="{ max: addDays(studentViewResponsesBefore, -1) }"
          />
          <div>
            and hide on
          </div>
          <custom-datetime-picker
            v-model="studentViewResponsesBefore"
            :datePickerProps="{ min: addDays(studentViewResponsesAfter, 1) }"
          />
        </div>
        <v-checkbox
          v-if="!isIntegration"
          v-model="allowStudentViewCorrectAnswers"
          class="allow-students-view-correct-answers"
          hide-details
        >
          <template
            v-slot:label
          >
            <span
              class="text--primary"
            >
              Allow students to see correct answers and any comments
            </span>
          </template>
        </v-checkbox>
        <div
          v-if="!isIntegration && allowStudentViewCorrectAnswers"
          class="correct-answer-date-controls d-flex flex-wrap align-baseline mt-5"
        >
          <div>
            Show correct answers and comments on
          </div>
          <custom-datetime-picker
            v-model="studentViewCorrectAnswersAfter"
            :datePickerProps="{
              min: convertDateToDateString(studentViewResponsesAfter),
              max:
                addDays(studentViewCorrectAnswersBefore, -1) ||
                convertDateToDateString(studentViewResponsesBefore),
            }"
          />
          <div>
            and hide on
          </div>
          <custom-datetime-picker
            v-model="studentViewCorrectAnswersBefore"
            :datePickerProps="{
              min:
                addDays(studentViewCorrectAnswersAfter, 1) ||
                convertDateToDateString(studentViewResponsesAfter),
              max: convertDateToDateString(studentViewResponsesBefore),
            }"
          />
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
import CustomDatetimePicker from "./CustomDatetimePicker.vue";

function createDateGetterSetter(prop) {
  return {
    // two-way computed property
    get() {
      // convert unix time number to date
      const unixTime = this.value[prop];
      return unixTime ? new Date(unixTime) : unixTime;
    },
    set(date) {
      // convert date to unix time number
      const unixTime = date ? date.getTime() : date;
      this.$emit("input", { ...this.value, [prop]: unixTime });
    },
  };
}
export default {
  components: { CustomDatetimePicker },
  // supports v-model
  props: {
    value: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      studentViewResponsesAfterMenu: null,
      studentViewResponsesBeforeMenu: null,
      studentViewCorrectAnswersAfterMenu: null,
      studentViewCorrectAnswersBeforeMenu: null
    };
  },
  computed: {
    integration() {
      return this.value.integration || null;
    },
    integrationConfiguration() {
      return this.isIntegration ? this.integration.configuration : {};
    },
    integrationClient() {
      return this.isIntegration ? this.integrationConfiguration.client : {};
    },
    integrationClientName() {
      return this.integrationClient.name;
    },
    isIntegration() {
      return this.integration !== null;
    },
    messages () {
      if (!this.isIntegration) {
        return {
          headerText: "Reveal treatment responses",
          headerInstructionsText: "Decide if students should see their treatment responses and points once a treatment question is answered",
          allowStudentViewResponsesLabel: "Allow students to see their treatment responses and points earned for each response",
          allowStudentViewResponsesDatesLabel: "Show responses and points on"
        }
      }

      switch(this.integrationClientName) {
        case "Custom Web Activity":
          return {
            headerText: "Reveal treatment responses",
            headerInstructionsText: "Decide if students should see their treatment responses and points once a treatment question is answered",
            allowStudentViewResponsesLabel: "Allow students to see their treatment responses and points earned for each response",
            allowStudentViewResponsesDatesLabel: "Show responses and points on"
          }
        default:
          return {
            headerText: "Reveal treatment scores",
            headerInstructionsText: "Decide if students should see their treatment points once a treatment question is answered",
            allowStudentViewResponsesLabel: "Allow students to see their treatment points earned for each response",
            allowStudentViewResponsesDatesLabel: "Show points on"
          }
      }
    },
    allowStudentViewResponses: {
      get() {
        return this.value.allowStudentViewResponses;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          allowStudentViewResponses: value,
        });
      },
    },
    studentViewResponsesAfter: createDateGetterSetter(
      "studentViewResponsesAfter"
    ),
    studentViewResponsesBefore: createDateGetterSetter(
      "studentViewResponsesBefore"
    ),
    allowStudentViewCorrectAnswers: {
      get() {
        return this.value.allowStudentViewCorrectAnswers;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          allowStudentViewCorrectAnswers: value,
        });
      },
    },
    studentViewCorrectAnswersAfter: createDateGetterSetter(
      "studentViewCorrectAnswersAfter"
    ),
    studentViewCorrectAnswersBefore: createDateGetterSetter(
      "studentViewCorrectAnswersBefore"
    ),
  },
  methods: {
    convertDateToDateString(date) {
      if (!date) {
        return date;
      }
      const month = String(date.getMonth() + 1).padStart(2, "0"); // getMonth returns zero-based month index
      const day = String(date.getDate()).padStart(2, "0");
      return `${date.getFullYear()}-${month}-${day}`; // ISO 8601 date format
    },
    addDays(date, days) {
      if (!date) {
        return date;
      }
      const updated = this.addDaysToDate(date, days);
      return this.convertDateToDateString(updated);
    },
    addDaysToDate(date, days) {
      const updated = new Date(date);
      updated.setDate(updated.getDate() + days);
      return updated;
    },
    changeAllowStudentViewResponses(value) {
      // It's important to emit an event that updates these in the parent component at once
      this.$emit("input", {
        ...this.value,
        allowStudentViewResponses: value,
        allowStudentViewCorrectAnswers: this.allowStudentViewCorrectAnswers && value,
      });
    },
  }
}
</script>

<style lang="scss" scoped>
.reveal-responses-card .v-card__text {
  font-size: 16px;
  margin-left: 32px;
}
.allow-students-view-correct-answers {
  margin-top: 30px;
}
.correct-answer-date-controls {
  margin-left: 32px;
}
</style>
