<template>
  <div>
    <h4>Reveal treatment responses</h4>
    <p class="grey--text text--darken-2 pb-0">
      Decide if students should see their treatment responses and points once a
      treatment question is answered
    </p>
    <v-card outlined class="reveal-responses-card">
      <v-card-title :class="{ 'blue lighten-5': allowStudentViewResponses }">
        <v-checkbox
          v-model="allowStudentViewResponses"
          class="mt-0"
          label="Allow students to see their treatment responses and points earned for each response"
          hide-details
          @change="changeAllowStudentViewResponses"
        ></v-checkbox>
      </v-card-title>
      <v-card-text v-if="allowStudentViewResponses" class="text--primary">
        <div class="d-flex flex-wrap align-baseline mt-5">
          <div>
            Show responses and points on
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
          class="allow-students-view-correct-answers"
          v-model="allowStudentViewCorrectAnswers"
          hide-details
        >
          <template v-slot:label>
            <span class="text--primary">
              Allow students to see correct answers and any comments</span
            >
          </template>
        </v-checkbox>
        <div
          v-if="allowStudentViewCorrectAnswers"
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
  props: ["value"],
  data() {
    return {
      studentViewResponsesAfterMenu: null,
      studentViewResponsesBeforeMenu: null,
      studentViewCorrectAnswersAfterMenu: null,
      studentViewCorrectAnswersBeforeMenu: null
    };
  },
  computed: {
    allowStudentViewResponses: {
      // two-way computed property
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
      // two-way computed property
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
        allowStudentViewCorrectAnswers:
          this.allowStudentViewCorrectAnswers && value,
      });
    },
  },
};
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
