<template>
  <div>
    <h4>Multiple attempts</h4>
    <p class="grey--text text--darken-2 pb-0">
      Decide if students should be able to submit the assignment more than once. 
    </p>
    <v-card outlined class="multiple-attempts-card">
      <v-card-title :class="{ 'blue lighten-5': allowMultipleAttempts }">
        <v-checkbox
          v-model="allowMultipleAttempts"
          class="mt-0"
          label="Allow multiple attempts"
          hide-details
        ></v-checkbox>
      </v-card-title>
      <v-card-text v-if="allowMultipleAttempts" class="text--primary mx-0 px-5">
        <div class="d-flex flex-wrap align-baseline">
          <v-radio-group
              v-model="allowInfiniteSubmissions"
              column
            >
              <v-radio
                label="blue"
                color="blue"
                class="mb-5"
                :value="false"
              >
                <template v-slot:label>
                    <div>A student is allowed up to <v-text-field
                            outlined
                            dense
                            single-line
                            hide-details
                            :required="!allowInfiniteSubmissions"
                            type="number"
                            class="d-inline-block"
                            :disabled="allowInfiniteSubmissions"
                            v-model="numOfSubmissions"
                            style="width:100px"
                        ></v-text-field> attempts
                    </div>
                </template>
              </v-radio>
              <v-radio
                label="A student is allowed an infinite number of attempts"
                color="blue"
                :value="true"
              ></v-radio>
          </v-radio-group>
        </div>
        <div class="mb-4"><v-divider class="mx-0 px-0"></v-divider></div>
        
        <div class="d-flex flex-column flex-wrap align-baseline">
            <div class="mb-5">Minimum time between submissions: <v-text-field
                    outlined
                    dense
                    single-line
                    hide-details
                    type="number"
                    class="d-inline-block"
                    v-model="hoursBetweenSubmissions"
                    style="width:100px"
                ></v-text-field> hours
            </div>
            <div class="mb-5">Keep the <v-select
                :items="cumulativeOptions"
                outlined
                single-line
                hide-details
                dense
                v-model="multipleSubmissionScoringScheme"
                class="d-inline-block"
                item-text="label"
                item-value="value"
                ></v-select> treatment score
            </div>
            <div class="mb-5" v-if="multipleSubmissionScoringScheme === 'CUMULATIVE'">Proportion earned on first attempt: <v-text-field
                    outlined
                    dense
                    single-line
                    hide-details
                    type="number"
                    class="d-inline-block"
                    v-model="cumulativeScoringInitialPercentage"
                    style="width:100px"
                ></v-text-field> %
            </div>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>
export default {
  components: { },
  // supports v-model
  props: ["value"],
  data() {
    return {
      cumulativeOptions: [
        {value: 'MOST_RECENT', label: 'Most Recent'},
        {value: 'HIGHEST', label: 'Highest'},
        {value: 'AVERAGE', label: 'Acerage'},
        {value: 'CUMULATIVE', label: 'Cumulative'},
      ]
    };
  },
  computed: {
    allowMultipleAttempts: {
      // two-way computed property
      get() {
        return this.value.numOfSubmissions === null ? false : true;
      },
      set(value) {
        if (value === true) {
            this.numOfSubmissions = 0;
        } else {
            this.numOfSubmissions = null;
        }
      },
    },
    allowInfiniteSubmissions: {
      get() {
        return this.value.numOfSubmissions > 0 ? false : true;
      },
      set(value) {
        if (value === true) {
            this.numOfSubmissions = 0;
        } else {
            this.numOfSubmissions = 1;
        }
      },
    },
    numOfSubmissions: {
      // two-way computed property
      get() {
        return this.value.numOfSubmissions;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          numOfSubmissions: value === null ? null : parseInt(value),
        });
      },
    },
    multipleSubmissionScoringScheme: {
      // two-way computed property
      get() {
        return this.value.multipleSubmissionScoringScheme;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          multipleSubmissionScoringScheme: value,
        });
      },
    },
    hoursBetweenSubmissions: {
      // two-way computed property
      get() {
        return this.value.hoursBetweenSubmissions;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          hoursBetweenSubmissions: parseFloat(value),
        });
      },
    },
    cumulativeScoringInitialPercentage: {
      // two-way computed property
      get() {
        return this.value.cumulativeScoringInitialPercentage;
      },
      set(value) {
        this.$emit("input", {
          ...this.value,
          cumulativeScoringInitialPercentage: parseFloat(value),
        });
      },
    },
  }
};
</script>

<style lang="scss" scoped>
.multiple-attempts-card .v-card__text {
  font-size: 16px;
  margin-left: 32px;
}

</style>
