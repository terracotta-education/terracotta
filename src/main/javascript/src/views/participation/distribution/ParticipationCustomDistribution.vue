<template>
  <div>
    <h1 class="mb-5">
      Select the percent of students you would like to receive each condition
    </h1>

    <div class="row mx-2">
      <div class="col-9 label">
        Condition
      </div>
      <div class="col-3 label">
        Distribution
      </div>
    </div>

    <v-card class="mt-2 pt-5 pr-5 mx-auto lighten-5 rounded-lg" outlined>
      <v-card-text
        v-for="(condition, index) in this.conditions"
        :key="condition.conditionId"
      >
        <v-row class="mr-2">
          <v-card-title class="col-10">
            {{ condition.name }} will receive
          </v-card-title>
          <v-text-field
            class="textfield"
            outlined
            suffix="%"
            v-model="distributionValue[index]"
            :rules="[(value) => value && !!value.trim() || 'Required']"
            required
          ></v-text-field>
        </v-row>
      </v-card-text>
    </v-card>

    <p v-if="isDisabled()" class="errorMessage mt-3">
      Please Provide Positive Value for Each Condition Distribution and All
      Condition Distributions should be equal to 100%.
    </p>

    <v-btn
      elevation="0"
      class="mt-3"
      :disabled="isDisabled()"
      color="primary"
      @click="updateDistribution()"
      >Continue
    </v-btn>
  </div>
</template>

<script>
import { mapActions } from "vuex";

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
    updateDistribution() {
      const updatedConditions = this.conditions.map((condition, index) => {
        return {
          ...condition,
          distributionPct: parseFloat(this.distributionValue[index]),
          experimentId: this.experimentId,
        };
      });

      this.updateConditions(updatedConditions)
        .then((response) => {
          if (response?.status === 200) {
            this.$router.push({
              name: "ParticipationSummary",
              params: { experiment: this.experimentId },
            });
          } else {
            alert(response.error);
          }
        })
        .catch((response) => {
          console.log("updateConditions | catch", { response });
        });
    },
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

.textfield {
  width: 15%;
}

.errorMessage {
  color: red;
}
</style>
