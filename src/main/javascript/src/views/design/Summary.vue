<template>
<div
  class="summary-container"
>
  <h1
    class="mb-3"
  >
    <span
      class="green--text font-weight-bold"
    >
      You've completed section 1.
    </span>
    <br>
    <span>Here's a summary of your experiment design.</span>
  </h1>
  <template
    v-if="experiment"
  >
    <div
      class="summary-panels"
    >
      <v-expansion-panels
        flat
      >
        <v-expansion-panel
          @click="panelExpansion"
          class="py-3 mb-3"
        >
          <v-expansion-panel-header><strong>Title</strong></v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>{{ experiment.title }}</p>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
      <v-expansion-panels
        flat
      >
        <v-expansion-panel
          @click="panelExpansion"
          class="py-3 mb-3"
        >
          <v-expansion-panel-header><strong>Description</strong></v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>{{ experiment.description }}</p>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
      <v-expansion-panels
        v-if="conditions && conditions.length > 0"
        flat
      >
        <v-expansion-panel
          @click="panelExpansion"
          class="py-3 mb-3"
        >
            <v-expansion-panel-header><strong>Conditions</strong></v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-list
                class="m-0 p-0"
              >
                <v-list-item
                  v-for="condition in conditions"
                  :key="condition.conditionId"
                  class="mx-0 px-0"
                >
                  <v-list-item-content>
                    <v-list-item-title v-text="condition.name"></v-list-item-title>
                  </v-list-item-content>
                  <v-list-item-icon>
                    <v-icon
                      v-if="condition.defaultCondition"
                      class="green--text"
                    >
                      mdi-check
                    </v-icon>
                  </v-list-item-icon>
                </v-list-item>
              </v-list>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
      <v-expansion-panels
        flat
      >
        <v-expansion-panel
          @click="panelExpansion"
          class="py-3 mb-6"
        >
          <v-expansion-panel-header><strong>Experiment Type</strong></v-expansion-panel-header>
          <v-expansion-panel-content>
            <p>{{ exposureType }}</p>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
    </div>
  </template>
  <v-btn
    v-if="!this.editMode"
    @click="nextSection"
    elevation="0"
    color="primary"
    class="mr-4"
  >
    Continue to next section
  </v-btn>
</div>
</template>

<script>
import {mapGetters} from "vuex";
import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

export default {
  name: "DesignSummary",
  props: {
    experiment: {
      type: Object,
      required: true
    }
  },
  computed: {
    ...mapGetters({
      conditions: "experiment/conditions",
      editMode: "navigation/editMode"
    }),
    exposureType() {
      return this.experiment.exposureType === "BETWEEN" ? "One condition" : "All conditions";
    },
    getSaveExitPage() {
      return this.editMode?.callerPage?.name || "Home";
    },
    getNextPage() {
      return this.editMode?.callerPage?.name || "ExperimentParticipationIntro";
    }
  },
  methods: {
    nextSection() {
      this.$router.push({
        name: this.getNextPage,
        params: {
          experiment: this.experiment.experimentId
        }
      })
    },
    saveExit() {
      this.$router.push({
        name: this.getSaveExitPage,
        params: {
          experiment: this.experiment.experimentId
        }
      })
    },
    panelExpansion() {
      setTimeout(() => {
        deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
      }, 1000);
    }
  },
  mounted() {
    deleteAttributesFromElement(".v-expansion-panel", ["aria-expanded"]);
  }
}
</script>

<style lang="scss" scoped>
.v-expansion-panel {
  border: 1px solid map-get($grey, "lighten-2");
}
</style>