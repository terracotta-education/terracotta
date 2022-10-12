<template>
  <div class="experiment-outcome" >
    <nav>
      <router-link
        v-if="this.$router.currentRoute.meta.previousStep"
        :to="{ name: this.$router.currentRoute.meta.previousStep }">
        <v-icon>mdi-chevron-left</v-icon> Back
      </router-link>
      <v-btn color="primary" elevation="0" class="saveButton" @click="$refs.childComponent.saveExit()">
        <span v-if="this.$router.currentRoute.meta.stepActionText">{{ this.$router.currentRoute.meta.stepActionText }}</span>
        <span v-else>SAVE & EXIT</span>
      </v-btn>
    </nav>
    <article class="experiment-outcome__body">
      <v-row>
        <v-col cols="12">
          <router-view :key="$route.fullPath" ref="childComponent" :experiment="experiment"></router-view>
        </v-col>
      </v-row>
    </article>
  </div>
</template>

<script>
import {mapGetters} from "vuex";
import store from "@/store";

export default {
  name: 'ExperimentOutcome',
  computed: {
    ...mapGetters({
      experiment: 'experiment/experiment',
    }),
    routeExperimentId() {
      return this.$route.params.experiment_id
    }
  },

  beforeRouteEnter (to, from, next) {
    return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
  },
  beforeRouteUpdate (to, from, next) {
    return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
  },
}
</script>

<style lang="scss" scoped>
@import '~vuetify/src/styles/main.sass';
@import '~@/styles/variables';

.experiment-outcome {
  min-height: 100%;

  > nav {
    padding: 30px;

    display: flex;
    justify-content: space-between;
    a {
      text-decoration: none;

      * {
        vertical-align: sub;
        @extend .blue--text;
      }
    }
    .saveButton {
      background: none!important;
      border: none;
      padding: 0!important;
      color: #069;
      cursor: pointer;
    }
  }
  > article {
    grid-area: article;
    padding: 0 30px;
  }
}
</style>