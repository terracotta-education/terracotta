<template>
  <div
    v-if="experiment && exposureId"
    class="outcome-gradebook-container"
  >
    <PageLoading
      v-if="!isLoaded"
      :display="!isLoaded"
      :message="`Loading gradebook items from ${lmsTitle}. Please wait.`"
    />

    <h1 class="mb-6">
      Select gradebook item(s)
    </h1>

    <form @submit.prevent="saveExit">
      <v-table class="mb-9 v-data-table--light-header">
        <thead>
          <tr>
            <th scope="col" style="width: 50px;">
              <v-checkbox
                v-model="selectAll"
                color="primary"
                aria-label="Select all gradebook items"
                hide-details
                @update:model-value="handleSelectAll"
              />
            </th>

            <th scope="col" class="text-left">
              Gradebook Item
            </th>

            <th
              scope="col"
              class="text-left"
              style="width: 250px;"
            >
              Total Points
            </th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="outcomePotential in outcomePotentials"
            :key="outcomePotential.assignmentId"
          >
            <td>
              <template v-if="!hasExistingOutcome(outcomePotential)">
                <v-checkbox
                  v-model="selectedAssignmentIds"
                  :value="outcomePotential.assignmentId"
                  :aria-label="`Select ${outcomePotential.name} for outcome creation`"
                  hide-details
                />
              </template>

              <template v-else>
                <v-icon>mdi-check</v-icon>
              </template>
            </td>

            <td>
              <!-- opens the details here rather than the LMS, so the selection isn't lost -->
              <button
                type="button"
                class="gradebook-item-name"
                aria-haspopup="dialog"
                @click="openDetails(outcomePotential, $event)"
              >
                {{ outcomePotential.name }}
              </button>
            </td>
            <td>{{ outcomePotential.pointsPossible }}</td>
          </tr>
        </tbody>
      </v-table>
    </form>

    <v-dialog
      v-model="detailsOpen"
      max-width="560px"
      content-class="gradebook-item-dialog"
      :content-props="{ style: { marginTop: dialogTop } }"
      aria-labelledby="gradebook-item-details-title"
      @after-leave="returnFocus"
    >
      <v-card
        v-if="selectedItem"
        ref="detailsCard"
      >
        <v-card-title class="gradebook-item-details-header">
          <h2
            id="gradebook-item-details-title"
            class="text-title-large"
          >
            {{ selectedItem.name }}
          </h2>

          <v-btn
            icon="mdi-close"
            variant="text"
            aria-label="Close"
            @click="detailsOpen = false"
          />
        </v-card-title>

        <v-card-text>
          <dl class="gradebook-item-details">
            <dt>Type</dt>
            <dd>{{ itemTypeLabel(selectedItem.type) }}</dd>

            <dt>Points</dt>
            <dd>{{ selectedItem.pointsPossible ?? "None" }}</dd>

            <dt>Due</dt>
            <dd>{{ dueLabel(selectedItem.dueAt) }}</dd>
          </dl>

          <template v-if="selectedItem.description">
            <h3 class="gradebook-item-description-heading">
              Description
            </h3>

            <p class="gradebook-item-description">
              {{ selectedItem.description }}
            </p>
          </template>
        </v-card-text>

        <v-card-actions>
          <v-btn
            v-if="lmsLink(selectedItem)"
            :href="lmsLink(selectedItem)"
            target="_blank"
            rel="noopener noreferrer"
            color="primary"
            variant="text"
            append-icon="mdi-open-in-new"
          >
            Open in {{ lmsTitle }}
            <span class="d-sr-only">(opens in a new tab)</span>
          </v-btn>

          <v-spacer />

          <v-btn
            variant="text"
            @click="detailsOpen = false"
          >
            Close
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  nextTick,
  onMounted
} from "vue";

import {
  useRoute,
  useRouter
} from "vue-router";

import Swal from "sweetalert2";

import dayjs from "@/plugins/dayjs";

import { statusAlert } from "@/helpers/ui-utils.js";
import PageLoading from "@/components/PageLoading.vue";

import { experiment as experimentModule } from "@/store/experiment.module";
import { outcome as outcomeModule } from "@/store/outcome.module";
import { alert as alertModule } from "@/store/alert.module";
import { configuration as configurationModule } from "@/store/configuration.module";

defineOptions({
  name: "OutcomeGradebook"
});

const route = useRoute();
const router = useRouter();

const experimentStore = experimentModule();
const outcomeStore = outcomeModule();
const alertStore = alertModule();
const configurationStore = configurationModule();

const selectedAssignmentIds = ref([]);
const detailsOpen = ref(false);
const selectedItem = ref(null);
const detailsCard = ref(null);
const POPUP_GAP = 16;
const dialogTop = ref(`${POPUP_GAP}px`);
let detailsOpener = null;
const selectAll = ref(false);
const isLoaded = ref(false);

const experiment = computed(() => {
  return experimentStore.experiment;
});

const outcomePotentials = computed(() => {
  return outcomeStore.outcomePotentials || [];
});

const outcomes = computed(() => {
  return outcomeStore.outcomes || [];
});

const alertStatuses = computed(() => {
  return alertStore.statuses;
});

const configurations = computed(() => {
  return configurationStore.configurations;
});

const exposureId = computed(() => {
  return route.params.exposureId;
});

const experimentId = computed(() => {
  return route.params.experimentId;
});

const lmsTitle = computed(() => {
  return configurations.value?.lmsTitle || "your LMS";
});

const hasExistingOutcome = outcomePotential => {
  return outcomes.value.some(outcome => {
    return (
      outcome.lmsOutcomeId === outcomePotential.assignmentId &&
      outcome.exposureId === exposureId.value
    );
  });
};

// the LMS's own names for what an item is (Canvas submission types)
const ITEM_TYPE_LABELS = {
  online_quiz: "Quiz",
  discussion_topic: "Discussion",
  external_tool: "External tool",
  wiki_page: "Page",
  not_graded: "Ungraded assignment"
};

const itemTypeLabel = type => ITEM_TYPE_LABELS[type] || "Assignment";

const dueLabel = dueAt => {
  return dueAt ? dayjs(dueAt).format("MMMM D, YYYY [at] h:mma") : "No due date";
};

// only for an ordinary LMS assignment or quiz: an LTI item (Terracotta's own, or another tool's)
// opens that tool instead. And only a web address: the link comes from the LMS
const lmsLink = item => {
  if (item?.terracotta) {
    return null;
  }

  return /^https?:\/\//i.test(item?.htmlUrl || "") ? item.htmlUrl : null;
};

// toward the top, level with the item clicked: in the LMS, Terracotta's frame grows to the whole page, so
// a centered dialog could open far below a long gradebook's visible part. Never above the top of the
// page, and never running off its bottom
const placeNearOpener = async () => {
  await nextTick();

  const openerTop = detailsOpener?.getBoundingClientRect?.().top ?? POPUP_GAP;
  const height = detailsCard.value?.$el?.offsetHeight || 0;
  const lowest = window.innerHeight - height - POPUP_GAP;

  dialogTop.value = `${Math.max(POPUP_GAP, Math.min(openerTop - POPUP_GAP, lowest))}px`;
};

const openDetails = (outcomePotential, event) => {
  detailsOpener = event?.currentTarget || null;
  selectedItem.value = outcomePotential;
  detailsOpen.value = true;
  void placeNearOpener();
};

// back to the item that opened it, for keyboard and screen reader users
const returnFocus = () => {
  detailsOpener?.focus();
  detailsOpener = null;
};

const selectableOutcomePotentials = computed(() => {
  return outcomePotentials.value.filter(
    outcomePotential => !hasExistingOutcome(outcomePotential)
  );
});

const handleSelectAll = () => {
  selectedAssignmentIds.value = selectAll.value
    ? selectableOutcomePotentials.value.map(
        outcomePotential => outcomePotential.assignmentId
      )
    : [];
};

const saveExit = async () => {
  try {
    const results = await Promise.all(
      selectedAssignmentIds.value.map(assignmentId => {
        const outcomePotential = outcomePotentials.value.find(outcome => {
          return outcome.assignmentId === assignmentId;
        });

        return outcomeStore.createOutcome([
          experimentId.value,
          exposureId.value,
          outcomePotential.name,
          outcomePotential.pointsPossible,
          true,
          outcomePotential.type,
          assignmentId
        ]);
      })
    );

    if (results.some(result => !result)) {
      throw new Error("Failed to create one or more outcomes");
    }

    let params = {};

    if (selectedAssignmentIds.value.length > 0) {
      params = {
        ...statusAlert(
          alertStatuses.value.success,
          "Outcomes created successfully."
        )
      };
    }

    router.push({
      name: "ExperimentSummary",
      params
    });
  } catch {
    Swal.fire({
      text: "An error occurred while creating outcomes. Please try again.",
      icon: "error"
    });
  }
};

onMounted(async () => {
  await outcomeStore.resetOutcomePotentials();
  await outcomeStore.fetchOutcomes([
    experimentId.value,
    exposureId.value
  ]);
  await outcomeStore.fetchOutcomePotentials(
    experimentId.value
  );

  isLoaded.value = true;
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.gradebook-item-name {
  padding: 0;
  border: 0;
  background: none;
  font: inherit;
  text-align: left;
  color: rgb(var(--v-theme-primary));
  text-decoration: underline;
  cursor: pointer;

  &:focus-visible {
    outline: 2px solid currentColor;
    outline-offset: 2px;
  }
}

.gradebook-item-details-header {
  display: flex;
  // lines the title up with the details below it (a card title is padded less than its text)
  padding-left: 24px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  white-space: normal;

  h2 {
    padding-top: 6px;
    font-weight: 500;
    word-break: break-word;
  }
}

.gradebook-item-details {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 8px 24px;
  margin: 0;

  dt {
    font-weight: 600;
  }

  dd {
    margin: 0;
  }
}

.gradebook-item-description-heading {
  margin: 20px 0 8px;
  font-size: 1rem;
  font-weight: 600;
}

.gradebook-item-description {
  max-height: 240px;
  overflow-y: auto;
  white-space: pre-line;
}
</style>

<style lang="scss">
/* Unscoped: the dialog is teleported to <body>, outside this component's DOM */
.v-dialog > .v-overlay__content.gradebook-item-dialog {
  align-self: flex-start;
}
</style>
