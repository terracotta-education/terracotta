<template>
  <div
    class="experiment-outcome"
  >
    <nav>
      <router-link
        v-if="this.$router.currentRoute.meta.previousStep"
        :disabled="isSaving"
        :to="getBackTo"
      >
        <v-icon>mdi-chevron-left</v-icon> Back
      </router-link>
      <v-btn
        :disabled="isSaving"
        @click="handleSaveClick()"
        color="primary"
        elevation="0"
        class="save-button"
      >
        <span
          v-if="this.$router.currentRoute.meta.stepActionText"
        >
          {{ this.$router.currentRoute.meta.stepActionText }}
        </span>
        <span
          v-else
        >
          SAVE & EXIT
        </span>
      </v-btn>
    </nav>
    <article
      class="experiment-outcome__body"
    >
      <v-row>
        <v-col
          cols="12"
        >
          <router-view
            :experiment="experiment"
            :key="$route.fullPath"
            ref="childComponent"
          ></router-view>
        </v-col>
      </v-row>
    </article>
  </div>
</template>

<script>
import {mapGetters} from "vuex";
import store from "@/store";

export default {
  name: "ExperimentOutcome",
  data: () => ({
    saveButtonClicked: false
  }),
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
    }),
    routeExperimentId() {
      return this.$route.params.experiment_id
    },
    isSaving() {
      return this.saveButtonClicked || false;
    },
    getBackTo() {
      if (this.isSaving) {
        return "";
      }

      return {
        name: this.$router.currentRoute.meta.previousStep
      };
    }
  },
  methods: {
    async handleSaveClick() {
      this.saveButtonClicked = true;
      await this.$refs.childComponent.saveExit();
      this.saveButtonClicked = false;
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
@import "~vuetify/src/styles/main.sass";
@import "~@/styles/variables";

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
    .save-button {
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
