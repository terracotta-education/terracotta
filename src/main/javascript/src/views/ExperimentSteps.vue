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
          <v-container
            :fluid="!narrowColumn"
            :class="{ 'steps-container--narrow': narrowColumn }"
          >
            <v-row :justify="narrowColumn ? 'center' : undefined">
              <v-col
                cols="12"
                :md="narrowColumn ? 6 : undefined"
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

// design and participation steps keep the centered, half-width column they had before the
// Vue 3 upgrade (TCOTA-1017); assignment steps, the builder and the editors use the full width
const narrowColumn = computed(() => ["design", "participation"].includes(currentSection.value));

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
    min-height: 50px;
    grid-area: nav;
    padding: 30px;
    display: flex;
    flex-wrap: wrap;
    row-gap: 16px;
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
    // not height: 100vh - sticky positioning doesn't need it (the sidebar sticks
    // within its grid area regardless), and forcing it here inflated the whole grid,
    // and therefore document.body, to a full viewport tall on short pages (the
    // assignment/message editors) even though their real content is much shorter -
    // see the .v-application__wrap comment in _global.scss for the other half of this.
    max-height: 100vh;
    grid-area: aside;
  }

  > article {
    grid-area: article;
    padding: 0;
  }

  // the Vuetify 2 container sizes the design and participation steps were laid out in before
  // the Vue 3 upgrade - Vuetify 3's own container padding and breakpoints differ slightly (16px
  // padding, 1200px at 1280px and up). Below 960px the column is full width. !important only to
  // outrank _global.scss's blanket `.v-container { max-width: 100% !important }`.
  .steps-container--narrow {
    padding: 12px;
    max-width: 100% !important;

    @media (min-width: 960px) {
      max-width: 900px !important;
    }

    @media (min-width: 1264px) {
      max-width: 1185px !important;
    }

    @media (min-width: 1904px) {
      max-width: 1785px !important;
    }
  }

  &__sidebar {
    background: map.get($grey, "lightest");
    padding: 30px 45px;
  }

  // below this, the "auto" sidebar column doesn't have room for its step
  // labels ("Section 1: Design", "Selection Method", etc.) next to the 1fr
  // article column, and a CSS grid's "auto" track won't shrink/wrap below
  // its content's own width - it forced the whole page to scroll
  // horizontally instead. Stack the sidebar above the article as a normal
  // (non-sticky, non-100vh) block so it collapses down instead. 636px
  // matches this app's existing mobile-table breakpoint (ComponentTable.vue).
  @media (max-width: 636px) {
    display: block;

    > aside {
      position: static;
      height: auto;
    }
  }
}
</style>
