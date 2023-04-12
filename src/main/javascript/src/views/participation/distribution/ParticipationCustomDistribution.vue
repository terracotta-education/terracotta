<template>
  <div>
    <h1 class="mb-5">
      Select the percent of students you would like to receive each condition
    </h1>

    <div class="row mx-2">
      <div class="col-9 label">
        Condition
      </div>
      <div class="col-3 label text-right">
        Distribution
      </div>
    </div>

    <v-card class="mt-2 mb-3 py-3 mx-auto lighten-5 rounded-lg" outlined>
      <v-card-text
        class="pa-5"
        v-for="(condition, index) in this.conditions"
        :key="condition.conditionId"
      >
        <v-row class="justify-space-between align-center">
          <v-col cols="9" class="py-0">
            <v-card-title class="ma-0 pa-0 body-1">
              {{ condition.name }} will receive
            </v-card-title>
          </v-col>
          <v-col cols="3" class="py-0">
            <v-text-field
              class="pa-0 ma-0 text-right"
              outlined
              suffix="%"
              v-model="distributionValue[index]"
              :rules="[(value) => !!value && !!value.trim()|| 'Required']"
              required
            ></v-text-field>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <p
      v-if="isDisabled()"
      class="errorMessage mt-3"
    >
      Please provide a positive value for each condition distribution and all condition sistributions should be equal to 100%.
    </p>

    <v-btn
      elevation="0"
      class="mt-3"
      :disabled="isDisabled()"
      color="primary"
      @click="updateDistribution('ParticipationSummary')"
    >
      Continue
    </v-btn>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";

export default {
  name: "ParticipationCustomDistribution",
  props: ["experiment"],

  data() {
    return {
      distributionValue: this.experiment.conditions.map(
        (condition) => condition.distributionPct
      ),
    };
  },

  computed: {
    ...mapGetters({
      editMode: 'navigation/editMode'
    }),
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || 'Home';
    },
    conditions() {
      return this.experiment.conditions;
    },
    totalDistribution() {
      return this.distributionValue
        .map((value) => +value)
        .reduce((acc, curr) => acc + curr, 0);
    },
    experimentId() {
      return this.experiment.experimentId;
    },
  },

  methods: {
    ...mapActions({
      updateConditions: "condition/updateConditions",
    }),
    isDisabled() {
      return (
        this.totalDistribution !== 100 ||
        this.distributionValue.some(
          (value) => parseInt(value) <= 0 || isNaN(parseInt(value))
        )
      );
    },
    updateDistribution(path) {
      const updatedConditions = this.conditions.map((condition, index) => {
        return {
          ...condition,
          distributionPct: parseFloat(this.distributionValue[index]),
          experimentId: this.experimentId,
        };
      });

      this.updateConditions(updatedConditions)
        .then((response) => {
          // Response will be array of objects
          if (response?.every(obj => obj.status === 200)) {
            this.$router.push({
              name: path,
              params: { experiment: this.experimentId },
            });
          } else {
            this.$swal(response.error);
          }
        })
        .catch((response) => {
          console.log("updateConditions | catch", { response });
        });
    },
    saveExit() {
       if (this.isDisabled()) {
          this.$router.push({
            name: this.getSaveExitPage,
            params: {
              experiment: this.experiment.experimentId
            }
          });
        } else {
          this.updateDistribution(this.getSaveExitPage);
        }
    }
  },
};
</script>

<style lang="scss">
.label {
  font-weight: 500;
  font-size: 12px;
  line-height: 16px;
  letter-spacing: 1.25px;
  text-transform: uppercase;
  color: #5f6368;
}

.v-input__slot {
  margin: 0;
}
.v-text-field__details {
  display: none;
}

.text-right input {
  text-align: right;
}

.errorMessage {
  color: red;
}
</style>
