<template>
  <div>
    <template
      v-if="experiment"
    >
      <div
        class="experiment-steps"
      >
        <aside
          v-if="!this.noSidebar.includes(this.$router.currentRoute.name)"
          class="experiment-steps__sidebar"
        >
          <steps
            :current-section="currentSection"
            :current-step="currentStep"
            :participationType="experiment.participationType"
          />
        </aside>
        <nav>
          <router-link
            v-if="this.editModePage"
            :to="getBackTo"
            :disabled="isSaving"
          >
            <v-icon>mdi-chevron-left</v-icon> Back
          </router-link>
          <v-btn
            v-show="this.$router.currentRoute.name !== 'ExperimentDesignIntro' && !isSaving"
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
              v-else-if="editMode"
            >
              SAVE & CLOSE
            </span>
            <span
              v-else
            >
              SAVE & EXIT
            </span>
          </v-btn>
        </nav>
        <article
          class="experiment-steps__body"
        >
          <v-container>
            <v-row
              justify="center"
            >
              <v-col
                md="6"
                class="steps-container-col"
              >
                <router-view
                  :key="$route.fullPath"
                  :experiment="experiment"
                  ref="childComponent"
                >
                </router-view>
              </v-col>
            </v-row>
          </v-container>
        </article>
      </div>
    </template>
    <template
      v-else
    >
      <v-row
        justify="center"
      >
        <v-col
          md="6"
        >
          <v-alert
            prominent
            type="error"
          >
            <v-row
              align="center"
            >
              <v-col
                class="grow"
              >
                Experiment not found
              </v-col>
            </v-row>
          </v-alert>
        </v-col>
      </v-row>
    </template>
  </div>
</template>

<script>
import {mapActions, mapGetters} from "vuex";
import Steps from "../components/Steps"
import store from "@/store"

export default {
  name: "ExperimentSteps",
  components: {
    Steps
  },
  data: () => ({
    saveButtonClicked: false
  }),
  computed: {
    ...mapGetters({
      experiment: "experiment/experiment",
      editMode: "navigation/editMode"
    }),
    currentSection() {
      return this.$router.currentRoute.meta.currentSection
    },
    currentStep() {
      return this.$router.currentRoute.meta.currentStep
    },
    routeExperimentId() {
      return this.$route.params.experimentId
    },
    noSidebar() {
      // these pages should not show the sidebar
      return [
        "TerracottaBuilder",
        "AssignmentCreateAssignment",
        "AssignmentEditor"
      ];
    },
    editModePage() {
      if (this.editMode?.initialPage === this.$router.currentRoute.name) {
        // this page is where we initially began, return to the caller page on exit
        return this.editMode.callerPage.name;
      }
      if (this.singleConditionExperiment && this.$router.currentRoute.meta.previousStepSingleCondition) {
        return this.$router.currentRoute.meta.previousStepSingleCondition;
      }
      return this.$router.currentRoute.meta.previousStep;
    },
    conditions() {
      return this.experiment.conditions;
    },
    singleConditionExperiment() {
      return this.conditions.length === 1;
    },
    isSaving() {
      return this.saveButtonClicked || false;
    },
    getBackTo() {
      if (this.isSaving) {
        return "";
      }

      return {
        name: this.editModePage
      };
    }
  },
  beforeRouteEnter (to, from, next) {
    // don't load new data after consent title screen
    if (from.name === "ParticipationTypeConsentTitle" && to.name === "ParticipationTypeConsentFile") {
      next();
      return;
    }
    return store.dispatch("experiment/fetchExperimentById", to.params.experimentId).then(next, next)
  },
  beforeRouteUpdate (to, from, next) {
    // don't load new data after consent title screen
    if (from.name === "ParticipationTypeConsentTitle" && to.name === "ParticipationTypeConsentFile") {
      next();
      return;
    }
    return store.dispatch("experiment/fetchExperimentById", to.params.experimentId).then(next, next)
  },
  methods: {
    ...mapActions({
      fetchExperimentById: "experiment/fetchExperimentById"
    }),
    async handleSaveClick() {
      this.saveButtonClicked = true;
      await this.$refs.childComponent.saveExit();
      this.saveButtonClicked = false;
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~vuetify/src/styles/main.sass";
@import "~@/styles/variables";

.experiment-steps {
  display: grid;
  min-height: 100%;
  grid-template-rows: auto 1fr;
  grid-template-columns: auto 1fr;
  grid-template-areas:
    "aside nav"
    "aside article";

  > nav {
    position: sticky;
    position: -webkit-sticky;
    top: 0;
    width: 100%;
    height: 50px;
    grid-area: nav;
    padding: 30px;
    display: flex;
    justify-content: space-between;
    z-index: 100;
    background: white;
    a {
      text-decoration: none;
      align-content: center;
      * {
        vertical-align: sub;
        @extend .blue--text;
      }
    }
    .save-button,
    .save-button:disabled,
    .save-button[disabled] {
      margin-left:auto;
      background: none!important;
      border: none;
      padding: 0!important;
      color: #069;
      cursor: pointer;
    }
    .save-button:disabled,
    .save-button[disabled] {
      color: grey;
    }
  }
  > aside {
    position: sticky;
    position: -webkit-sticky;
    top: 0;
    height: 100vh;
    grid-area: aside;
  }
  > article {
    grid-area: article;
    padding: 0;
    padding-top: 100px !important;
  }

  &__sidebar {
    background: map-get($grey, 'lighten-4');
    padding: 30px 45px;
  }
}
</style>
