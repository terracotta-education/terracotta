<template>
  <div>
    <h4>Reveal treatment responses</h4>
    <p class="grey--text text--darken-2">
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
        ></v-checkbox>
      </v-card-title>
      <v-card-text v-if="allowStudentViewResponses" class="text--primary">
        <div class="d-flex flex-wrap align-baseline mt-5">
          <div>
            Show responses and points on
          </div>
          <v-menu v-model="studentViewResponsesAfterMenu" min-width="auto">
            <template v-slot:activator="{ on, attrs }">
              <v-text-field
                v-model="studentViewResponsesAfterFormatted"
                class="date-field"
                append-icon="mdi-calendar"
                readonly
                outlined
                dense
                hide-details
                v-bind="attrs"
                v-on="on"
              ></v-text-field>
            </template>
            <v-date-picker
              v-model="studentViewResponsesAfter"
              @input="studentViewResponsesAfterMenu = false"
              :max="studentViewResponsesBefore"
            >
              <v-spacer></v-spacer>
              <v-btn
                text
                color="primary"
                @click="studentViewResponsesAfter = null"
              >
                Clear
              </v-btn>
            </v-date-picker>
          </v-menu>
          <div>
            and hide on
          </div>
          <v-menu v-model="studentViewResponsesBeforeMenu" min-width="auto">
            <template v-slot:activator="{ on, attrs }">
              <v-text-field
                v-model="studentViewResponsesBeforeFormatted"
                class="date-field"
                append-icon="mdi-calendar"
                readonly
                dense
                hide-details
                outlined
                v-bind="attrs"
                v-on="on"
              ></v-text-field>
            </template>
            <v-date-picker
              v-model="studentViewResponsesBefore"
              @input="studentViewResponsesBeforeMenu = false"
              :min="studentViewResponsesAfter"
            >
              <v-spacer></v-spacer>
              <v-btn
                text
                color="primary"
                @click="studentViewResponsesBefore = null"
              >
                Clear
              </v-btn>
            </v-date-picker>
          </v-menu>
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
          <v-menu v-model="studentViewCorrectAnswersAfterMenu" min-width="auto">
            <template v-slot:activator="{ on, attrs }">
              <v-text-field
                v-model="studentViewCorrectAnswersAfterFormatted"
                class="date-field"
                append-icon="mdi-calendar"
                readonly
                outlined
                dense
                hide-details
                v-bind="attrs"
                v-on="on"
              ></v-text-field>
            </template>
            <v-date-picker
              v-model="studentViewCorrectAnswersAfter"
              @input="studentViewCorrectAnswersAfterMenu = false"
              :min="studentViewResponsesAfter"
              :max="
                studentViewCorrectAnswersBefore || studentViewResponsesBefore
              "
            >
              <v-spacer></v-spacer>
              <v-btn
                text
                color="primary"
                @click="studentViewCorrectAnswersAfter = null"
              >
                Clear
              </v-btn>
            </v-date-picker>
          </v-menu>
          <div>
            and hide on
          </div>
          <v-menu
            v-model="studentViewCorrectAnswersBeforeMenu"
            min-width="auto"
          >
            <template v-slot:activator="{ on, attrs }">
              <v-text-field
                v-model="studentViewCorrectAnswersBeforeFormatted"
                class="date-field"
                append-icon="mdi-calendar"
                readonly
                dense
                hide-details
                outlined
                v-bind="attrs"
                v-on="on"
              ></v-text-field>
            </template>
            <v-date-picker
              v-model="studentViewCorrectAnswersBefore"
              @input="studentViewCorrectAnswersBeforeMenu = false"
              :min="studentViewCorrectAnswersAfter || studentViewResponsesAfter"
              :max="studentViewResponsesBefore"
            >
              <v-spacer></v-spacer>
              <v-btn
                text
                color="primary"
                @click="studentViewCorrectAnswersBefore = null"
              >
                Clear
              </v-btn>
            </v-date-picker>
          </v-menu>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
function createDateGetterSetter(prop) {
  return {
    // two-way computed property
    get() {
      // convert unix time number to date string
      const unixTime = this.value[prop];
      return unixTime ? this.convertUnixTimeToDateString(unixTime) : unixTime;
    },
    set(dateString) {
      // convert date string to unix time number
      const unixTime = dateString
        ? this.convertDateStringToUnixTime(dateString)
        : dateString;
      this.$emit("input", { ...this.value, [prop]: unixTime });
    },
  };
}
export default {
  // supports v-model
  props: ["value"],
  data() {
    return {
      studentViewResponsesAfterMenu: null,
      studentViewResponsesBeforeMenu: null,
      studentViewCorrectAnswersAfterMenu: null,
      studentViewCorrectAnswersBeforeMenu: null,
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
    studentViewResponsesAfterFormatted() {
      return this.studentViewResponsesAfter
        ? this.convertDateStringToDate(
            this.studentViewResponsesAfter
          ).toLocaleDateString(undefined, {
            dateStyle: "short",
          })
        : null;
    },
    studentViewResponsesBefore: createDateGetterSetter(
      "studentViewResponsesBefore"
    ),
    studentViewResponsesBeforeFormatted() {
      return this.studentViewResponsesBefore
        ? this.convertDateStringToDate(
            this.studentViewResponsesBefore
          ).toLocaleDateString(undefined, {
            dateStyle: "short",
          })
        : null;
    },
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
    studentViewCorrectAnswersAfterFormatted() {
      return this.studentViewCorrectAnswersAfter
        ? this.convertDateStringToDate(
            this.studentViewCorrectAnswersAfter
          ).toLocaleDateString(undefined, {
            dateStyle: "short",
          })
        : null;
    },
    studentViewCorrectAnswersBefore: createDateGetterSetter(
      "studentViewCorrectAnswersBefore"
    ),
    studentViewCorrectAnswersBeforeFormatted() {
      return this.studentViewCorrectAnswersBefore
        ? this.convertDateStringToDate(
            this.studentViewCorrectAnswersBefore
          ).toLocaleDateString(undefined, {
            dateStyle: "short",
          })
        : null;
    },
  },
  methods: {
    convertDateStringToDate(dateString) {
      // Assumption: dateString is in the format "YYYY-MM-DD"

      // Deliberately not including time zone offset in string. When time zone
      // offset is absent, but date and time are present in the string, the time
      // zone is interpreted as local time.  See:
      // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/parse#date_time_string_format
      const date = new Date(dateString + "T00:00:00"); // midnight local time
      return date;
    },
    convertDateStringToUnixTime(dateString) {
      // Assumption: dateString is in the format "YYYY-MM-DD"
      const date = this.convertDateStringToDate(dateString);
      return date.getTime();
    },
    convertUnixTimeToDateString(unixTime) {
      const date = new Date(unixTime);
      const month = String(date.getMonth() + 1).padStart(2, "0"); // getMonth returns zero-based month index
      const day = String(date.getDate()).padStart(2, "0");
      return `${date.getFullYear()}-${month}-${day}`; // ISO 8601 date format
    },
  },
};
</script>

<style lang="scss" scoped>
.date-field {
  flex: 0 1 120px;
  margin-left: 0.5rem;
  margin-right: 0.5rem;
}
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
