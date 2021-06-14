<template>
  <div>
    <h1>
      Because you have <strong>two conditions</strong> and would like your
      students to be
      <strong>exposed to every conditions</strong>(within-subject), we will set
      you up with two exposure sets.
    </h1>

    <div class="mt-3">
      <strong> Exposre Set: </strong>
      <v-btn-toggle
        v-model="toggle_exclusive"
        class="ml-3"
        color="primary"
        mandatory
      >
        <v-btn v-for="item in [1, 2, 3]" :key="item">{{ item }}</v-btn>
      </v-btn-toggle>
    </div>

    <v-card class="mt-5 pt-5 px-5 mx-auto lighten-5 rounded-lg" outlined>
      <v-card-title
        >Group A will receive
        <v-chip class="ma-2" color="primary">Your Condidtion A</v-chip>
      </v-card-title>
      <v-card-title
        >Group B will receive
        <v-chip class="ma-2" color="primary">Your Condidtion B</v-chip>
      </v-card-title>
    </v-card>

    <v-btn
      class="mt-5"
      elevation="0"
      color="primary"
      :to="{ name: 'AssignmentExposureSets' }"
      >Continue
    </v-btn>
  </div>
</template>

<script>
import { mapActions } from "vuex";
import store from "@/store";

export default {
  name: "AssignmentExposureSets",
  props: ["experiment"],
  computed: {
    exposures() {
      return this.$store.state.exposures;
    },
  },
  methods: {
    ...mapActions({
      fetchExposures: "exposures/fetchExposures",
    }),
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
