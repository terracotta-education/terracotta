<template>
  <div class="experiment-outcome" >
    <nav>
      <router-link
        v-if="this.$router.currentRoute.meta.previousStep"
        :to="{ name: this.$router.currentRoute.meta.previousStep }">
        <v-icon>mdi-chevron-left</v-icon> Back
      </router-link>
      <v-btn color="primary" elevation="0" class="saveButton" @click="$refs.childComponent.saveExit()">SAVE & EXIT</v-btn>
    </nav>
    <router-view :key="$route.fullPath" ref="childComponent" :experiment="experiment"></router-view>
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
    // don't load new data after consent title screen
    if (from.name==='ParticipationTypeConsentTitle' && to.name==='ParticipationTypeConsentFile') { next(); return;}
    return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
  },
  beforeRouteUpdate (to, from, next) {
    // don't load new data after consent title screen
    if (from.name==='ParticipationTypeConsentTitle' && to.name==='ParticipationTypeConsentFile') { next(); return;}
    return store.dispatch('experiment/fetchExperimentById', to.params.experiment_id).then(next, next)
  },
}
</script>

<style lang="scss" scoped>
@import '~vuetify/src/styles/main.sass';
@import '~@/styles/variables';

.experiment-steps {
  display: grid;
  min-height: 100%;
  grid-template-rows: auto 1fr;
  grid-template-columns: auto 1fr;
  grid-template-areas:
			"nav"
			"article";

  > nav {
    grid-area: nav;
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
  > aside {
    grid-area: aside;
  }
  > article {
    grid-area: article;
    padding: 0;
  }

  &__sidebar {
    background: map-get($grey, 'lighten-4');
    padding: 30px 45px;
  }
  &__body {
  }
}
</style>