<template>
  <div>
    <template v-if="experiment">
      <div class="experiment-steps">
        <aside
          v-if="!noSidebar.includes(route.name)"
          class="experiment-steps__sidebar"
        >
          <Steps
            :current-section="currentSection"
            :current-step="currentStep"
            :participation-type="experiment.participationType"
          />
        </aside>

        <nav
          class="d-flex align-center"
        >
          <router-link
            v-if="editModePage"
            :to="getBackTo"
            :disabled="isSaving"
          >
            <v-icon>mdi-chevron-left</v-icon>
            Back
          </router-link>

          <div
            class="nav-right d-flex align-center"
            :class="{ 'ml-auto': !editModePage }"
          >
            <v-btn
              v-show="route.name !== 'ExperimentDesignIntro' && !isSaving"
              :disabled="isSaving"
              color="primary"
              elevation="0"
              class="save-button"
              @click="handleSaveClick"
            >
              <span v-if="route.meta.stepActionText">
                {{ route.meta.stepActionText }}
              </span>

              <span v-else-if="editMode">
                SAVE & CLOSE
              </span>

              <span v-else>
                SAVE & EXIT
              </span>
            </v-btn>

            <Help />
          </div>
        </nav>

        <article class="experiment-steps__body">
          <v-container fluid>
            <v-row>
              <v-col
                cols="12"
                class="steps-container-col"
              >
                <router-view
                  v-slot="{ Component }"
                >
                  <component
                    :is="Component"
                    :key="route.fullPath"
                    :experiment="experiment"
                    ref="childComponent"
                  />
                </router-view>
              </v-col>
            </v-row>
          </v-container>
        </article>
      </div>
    </template>

    <template v-else>
      <v-row justify="center">
        <v-col md="8">
          <v-alert
            type="error"
            variant="outlined"
          >
            <v-row align="center">
              <v-col class="grow">
                Experiment not found
              </v-col>
            </v-row>
          </v-alert>
        </v-col>
      </v-row>
    </template>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted
} from "vue";

import {
  useRoute,
  onBeforeRouteUpdate
} from "vue-router";

import Help from "@/components/Help.vue";
import Steps from "@/components/Steps.vue";

import { experiment as experimentModule } from "@/store/experiment.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ExperimentSteps"
});

const route = useRoute();

const experimentStore = experimentModule();
const navigationStore = navigationModule();

const childComponent = ref(null);
const saveButtonClicked = ref(false);

const experiment = computed(() => experimentStore.experiment);
const editMode = computed(() => navigationStore.editMode);

const currentSection = computed(() => {
  return route.meta.currentSection;
});

const currentStep = computed(() => {
  return route.meta.currentStep;
});

const noSidebar = [
  "TerracottaBuilder",
  "AssignmentCreateAssignment",
  "AssignmentEditor",
  "Message",
  "MessageContainer"
];

const conditions = computed(() => {
  return experiment.value?.conditions || [];
});

const singleConditionExperiment = computed(() => {
  return conditions.value.length === 1;
});

const editModePage = computed(() => {
  if (editMode.value?.initialPage === route.name) {
    return editMode.value.callerPage.name;
  }

  if (
    singleConditionExperiment.value &&
    route.meta.previousStepSingleCondition
  ) {
    return route.meta.previousStepSingleCondition;
  }

  return route.meta.previousStep;
});

const isSaving = computed(() => {
  return saveButtonClicked.value || false;
});

const getBackTo = computed(() => {
  if (isSaving.value) {
    return "";
  }

  return {
    name: editModePage.value
  };
});

const shouldSkipFetch = (to, from) => {
  return (
    from.name === "ParticipationTypeConsentTitle" &&
    to.name === "ParticipationTypeConsentFile"
  );
};

const fetchExperiment = async routeToUse => {
  await experimentStore.fetchExperimentById(
    routeToUse.params.experimentId
  );
};

const handleSaveClick = async () => {
  saveButtonClicked.value = true;

  try {
    await childComponent.value?.saveExit?.();
  } finally {
    saveButtonClicked.value = false;
  }
};

onMounted(async () => {
  await fetchExperiment(route);
});

onBeforeRouteUpdate(async (to, from, next) => {
  if (shouldSkipFetch(to, from)) {
    next();
    return;
  }

  await fetchExperiment(to);
  next();
});
</script>

<style lang="scss" scoped>
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
    align-items: center;
    z-index: 100;
    background: white;

    a {
      text-decoration: none;
      align-content: center;

      * {
        vertical-align: sub;
        color: map.get($blue, "primary");
      }
    }

    .save-button,
    .save-button:disabled,
    .save-button[disabled] {
      margin-left: auto;
      background: none !important;
      border: none;
      padding: 0 !important;
      color: map.get($blue, "primary");
      cursor: pointer;
    }

    .save-button:disabled,
    .save-button[disabled] {
      color: grey;
    }

    > .nav-right {
      display: flex;
      justify-content: right;
      max-width: fit-content;
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
  }

  &__sidebar {
    background: map.get($grey, "lightest");
    padding: 30px 45px;
  }
}
</style>
