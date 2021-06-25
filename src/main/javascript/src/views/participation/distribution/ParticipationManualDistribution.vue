<template>
  <div>
    <h1 class="mb-5">
      Select which students you would like for each condition.
    </h1>

    <!-- Conditions Section -->
    <p>Conditions</p>
    <v-expansion-panels class="v-expansion-panels--icon" flat>
      <v-expansion-panel
        v-for="(condition, index) in this.conditions"
        :key="condition.conditionId"
      >
        <v-expansion-panel-header>
          {{ condition.name }} (0)
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ListParticipants
            :listOfParticipants="arrayData[index]"
            :moveToOptions="getConditionNames"
            :moveToHandler="moveToHandler"
            :selectedOption="'' + index"
          />
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>

    <!-- Unassigned -->
    <ListParticipants
      :listOfParticipants="arrayData[this.getConditionNames.length - 1]"
      :moveToOptions="getConditionNames"
      :moveToHandler="moveToHandler"
      :selectedOption="'' + (this.getConditionNames.length - 1)"
    />

    <!-- {{ this.arrayData[2] }}
    <br />
    {{ this.conditions }}
    <br />
    {{ this.participants }}
    <br />

    {{ this.experiment }}

    <br /> -->
    <v-btn elevation="0" class="mt-3" color="primary">Continue</v-btn>
  </div>
</template>

<script>
import store from "@/store";
import { mapGetters } from "vuex";
import ListParticipants from "../../../components/ListParticipants.vue";

export default {
  name: "ParticipationManualDistribution",
  props: ["experiment"],
  components: {
    ListParticipants,
  },
  data() {
    return {
      //   arrayD: [[], [], [...this.participants]],
    };
  },
  computed: {
    ...mapGetters({
      participants: "participants/participants",
    }),

    conditions() {
      return this.experiment.conditions;
    },

    getConditionNames() {
      return [
        ...this.experiment.conditions.map((condition) => condition.name),
        "Unassigned",
      ];
    },
    // arrayData() {
    //   return (index) => {
    //     console.log(index);
    //     return [[],[],this.participants];
    //   };
    // },
    arrayData: {
      get: function(index) {
        console.log("In Get");
        const _this = this;
        console.log(index);
        return [[], [], [..._this.participants]];
      },
      set: function(newValue) {
        console.log("newValue: ", newValue);
        return newValue;
      },
    },

    defaultParticipantsToConditions() {
      const temp = [];
      this.participants.forEach(() => {
        temp.push([]);
      });
      return [...temp, this.participants];
    },
  },
  methods: {
    getParticipants(index) {
      return this.defaultParticipantsToConditions[index]
        ? this.defaultParticipantsToConditions[index]
        : this.defaultParticipantsToConditions;
    },

    moveToHandler(option, tempSelected) {
      console.log("Here", option, JSON.stringify(tempSelected));

      this.arrayData = [[...this.participants], [], []];
      //   this.title = "Hello2";
    },
  },
  beforeUpdate() {
    console.log("beforeUpdate");
  },
  updated() {
    console.log("update");
  },
  beforeRouteEnter(to, from, next) {
    // don't load new data after participant selection screen
    return store
      .dispatch("participants/fetchParticipants", to.params.experiment_id)
      .then(next, next);
  },
  beforeRouteUpdate(to, from, next) {
    // don't load new data after participant selection screen
    return store
      .dispatch("participants/fetchParticipants", to.params.experiment_id)
      .then(next, next);
  },
};
</script>
