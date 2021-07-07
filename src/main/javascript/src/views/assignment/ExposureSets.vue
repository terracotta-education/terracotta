<template>
  <div>
    <h1>
      Because you have
      <strong>{{ numberOfConditions }} conditions</strong> and would like your
      students to be
      <template v-if="exposureType === 'WITHIN'">
        <strong>exposed to every conditions</strong> (within)
      </template>
      <template v-else>
        <strong>exposed to only one condition</strong>(between)
      </template>
      , we will set you up with {{ numberOfExperimentSets }} exposure sets.
    </h1>

    <div class="mt-3">
      <strong> Exposure Set: </strong>
      <v-btn-toggle
        v-model="selectedExposure"
        class="ml-3"
        color="primary"
        mandatory
      >
        <v-btn
          v-for="(item, index) in exposures"
          :value="item"
          :key="item.exposureId"
          >{{ index + 1 }}</v-btn
        >
      </v-btn-toggle>
    </div>

    <v-card class="mt-5 pa-5 mx-auto lighten-5 rounded-lg" outlined>
      <p class="pa-0 my-0" v-for="group in sortedGroups()" :key="group">
        {{ group }} will receive
        <v-chip class="ma-2" color="primary" label>
          {{ groupNameConditionMapping[group] }}</v-chip
        >
      </p>
    </v-card>


    <v-btn
      class="mt-5"
      elevation="0"
      color="primary"
      :to="{ name: 'AssignmentExposureSetsIntro', params: {numberOfExperimentSets: this.numberOfExperimentSets } }"
      >Continue
    </v-btn>
  </div>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import store from "@/store";

export default {
  name: "AssignmentExposureSets",
  props: ["experiment"],
  data() {
    return {
      selectedExposure: [],
    };
  },
  computed: {
    ...mapGetters({
      exposures: "exposures/exposures",
    }),
    exposureType() {
      return this.experiment.exposureType;
    },
    numberOfConditions() {
      return this.experiment.conditions.length;
    },
    numberOfExperimentSets() {
      return this.exposures.length;
    },
    groupNameConditionMapping() {
      const groupConditionMap = {};
      this.selectedExposure.groupConditionList?.map(
        (group) => (groupConditionMap[group.groupName] = group.conditionName)
      );
      return groupConditionMap;
    },
  },
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
    }),

    sortedGroups() {
      const newGroups = this.selectedExposure?.groupConditionList?.map(
        (group) => group.groupName
      );
      return newGroups?.sort();
    },
  },
  beforeRouteEnter(to, from, next) {
    // don't load new data after consent title screen
    return store
      .dispatch("exposures/fetchExposures", to.params.experiment_id)
      .then(next, next);
  },
  beforeRouteUpdate(to, from, next) {
    // don't load new data after consent title screen
    return store
      .dispatch("exposures/fetchExposures", to.params.experiment_id)
      .then(next, next);
  },
};
</script>
