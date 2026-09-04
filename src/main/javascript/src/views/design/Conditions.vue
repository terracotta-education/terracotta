<template>
<div>
  <h1>Name your conditions</h1>
  <p>These will be used to label the different experimental versions of your assignments.</p>
  <v-form
    @submit.prevent="saveConditions(nextPage, true)"
    class="my-5 mb-15"
    ref="conditionsForm"
  >
    <v-container
      class="pa-0"
    >
      <v-row
        v-for="(orderedCondition, i) in orderedConditions"
        :key="orderedCondition.conditionId"
        density="compact"
      >
        <v-col
          class="py-0"
        >
          <v-text-field
              v-model="orderedCondition.name"
              :name="'condition-' + orderedCondition.conditionId"
              :rules="[duplicateRule(orderedCondition), requiredRule(orderedCondition), maxLengthRule(orderedCondition)]"
              label="Condition name"
              placeholder="e.g. Condition Name"
              variant="outlined"
              required
          >
          </v-text-field>
        </v-col>
        <v-col
          v-if="deleteAllowed && i > 0"
          class="py-0"
          cols="4"
          sm="2"
        >
          <v-btn
            :aria-label="`Delete condition ${orderedCondition.name || i + 1}`"
            @click="handleDeleteCondition(orderedCondition)"
            class="delete_condition"
            variant="outlined"
            icon
            rounded="0"
          >
            <v-icon>mdi-delete</v-icon>
          </v-btn>
        </v-col>
      </v-row>
    </v-container>
    <div
      v-if="addAllowed"
    >
      <v-btn
        v-if="conditions.length < 16"
        @click="createNewCondition()"
        color="blue"
        class="add_condition px-0 mb-2"
        variant="text"
      >
        Add another condition
      </v-btn>
      <v-alert
        v-else
        type="error"
        variant="outlined"
      >
        You have reached the maximum number of conditions (16) allowed by the experiment builder.
      </v-alert>
    </div>
    <v-btn
      :disabled="hasFieldErrors"
      elevation="0"
      color="primary"
      class="mr-4"
      type="submit"
    >
      Next
    </v-btn>
  </v-form>
  <v-card
    v-if="singleConditionExperiment && deleteAllowed"
    class="mt-6 pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg"
    variant="outlined"
  >
    <p>
      Once you click NEXT to leave this screen, you will be able to add, but not delete conditions,
      so please double-check that you have included what you need. To change your decisions beyond this point,
      you will need to create a new experiment.
    </p>
  </v-card>
  <v-card
    v-if="!deleteAllowed"
    class="mt-6 pt-5 px-5 mx-auto bg-blue-lighten-5 rounded-lg"
    variant="outlined"
  >
    <p>
      Please note that you are not able to {{ !addAllowed ? "add or" : "" }} delete conditions,
      as you have previously completed {{ !addAllowed ? "your experiment design and participation settings" : "this section"}}.
      To {{ !addAllowed ? "add or" : "" }} delete conditions, please create a new experiment.
    </p>
  </v-card>
</div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted,
  createApp
} from "vue";

import {
  useRoute,
  useRouter,
  onBeforeRouteUpdate
} from "vue-router";

import Swal from "sweetalert2";

import ConditionDeleteAlert from "@/components/ConditionDeleteAlert.vue";

import { api as apiModule } from "@/store/api.module";
import { condition as conditionModule } from "@/store/condition.module";
import { experiment as experimentModule } from "@/store/experiment.module";
import { exposures as exposuresModule } from "@/store/exposures.module";
import { groups as groupsModule } from "@/store/groups.module";
import { navigation as navigationModule } from "@/store/navigation.module";
import { storeToRefs } from "pinia";

defineOptions({
  name: "DesignConditions"
});

const route = useRoute();
const router = useRouter();

const conditionsForm = ref(null);

const apiStore = apiModule();
const conditionStore = conditionModule();
const experimentStore = experimentModule();
const exposuresStore = exposuresModule();
const groupsStore = groupsModule();
const navigationStore = navigationModule();

const { experiment, conditions } = storeToRefs(experimentStore);

const fieldErrors = ref({
  duplicateName: {
    conditionIds: [],
    message: "Multiple conditions have the same name."
  },
  requiredName: {
    conditionIds: [],
    message: "A name is required for each condition."
  },
  maxLengthName: {
    conditionIds: [],
    message:
      "A maximum of 255 characters is allowed for condition names."
  }
});

const hasFieldErrors = ref(false);

const editMode = computed(
  () => navigationStore.editMode
);

const saveExitPage = computed(
  () => editMode.value?.callerPage?.name || "Home"
);

const nextPage = computed(() =>
  singleConditionExperiment.value
    ? "ExperimentDesignSummary"
    : "ExperimentDesignType"
);

const addAllowed = computed(
  () => !editMode.value
);

const deleteAllowed = computed(
  () => experiment.value?.exposureType === "NOSET"
);

const orderedConditions = computed(() =>
  [...(conditions.value || [])].sort(
    (a, b) => a.conditionId - b.conditionId
  )
);

const singleConditionRemainsAfterDelete =
  computed(() => conditions.value?.length === 2);

const singleConditionExperiment = computed(
  () => conditions.value?.length === 1
);

const experimentId = computed(
  () => experiment.value?.experimentId
);

const duplicateName = computed(
  () =>
    fieldErrors.value.duplicateName.conditionIds
);

const requiredName = computed(
  () =>
    fieldErrors.value.requiredName.conditionIds
);

const maxLengthName = computed(
  () =>
    fieldErrors.value.maxLengthName.conditionIds
);

const errorMessage = computed(() => {
  if (duplicateName.value.length) {
    return fieldErrors.value.duplicateName.message;
  }

  if (requiredName.value.length) {
    return fieldErrors.value.requiredName.message;
  }

  if (maxLengthName.value.length) {
    return fieldErrors.value.maxLengthName.message;
  }

  return "Unspecified error.";
});

watch(
  conditions,
  () => {
    clearFieldErrors();
    conditionsForm.value?.validate();
  },
  { deep: true }
);

watch(
  fieldErrors,
  () => {
    hasFieldErrors.value =
      calculateFieldErrors();
  },
  { deep: true }
);

async function createNewCondition() {
  let doAdd = deleteAllowed.value;

  if (!deleteAllowed.value) {
    const reallyAdd = await Swal.fire({
      icon: "question",
      text: "Do you really want to add a new condition? You will not be able to delete it.",
      showCancelButton: true,
      confirmButtonText: "Yes, add it",
      cancelButtonText: "No, cancel",
      cancelButtonColor: "#515961"
    });

    doAdd = reallyAdd.isConfirmed;
  }

  if (doAdd) {
    await conditionStore.createCondition(
      experimentId.value
    );

    conditionsForm.value?.validate();
  }
}

async function saveConditions(
  path,
  updateExperimentFlag
) {
  if (hasFieldErrors.value) {
    await Swal.fire(
      `There was an error saving your conditions. ${errorMessage.value}`
    );
    return;
  }

  if (singleConditionExperiment.value) {
    conditions.value[0].defaultCondition = true;
  }

  const response =
    await conditionStore.updateConditions(
      conditions.value
    );

  if (response?.status === 200) {
    if (
      singleConditionExperiment.value &&
      updateExperimentFlag
    ) {
      await updateConditionExperiment(
        "BETWEEN",
        "EVEN",
        "exposure_type"
      );
    }

    router.push({
      name: path
    });
  } else {
    await Swal.fire(
      "There was an error saving your conditions."
    );
  }
}

async function updateConditionExperiment(
  exposureType,
  distributionType,
  step
) {
  experiment.value.exposureType = exposureType;
  experiment.value.distributionType = distributionType;

  const response = await experimentStore.updateExperiment(experiment.value);

  if (response?.status === 200) {
    if (!editMode.value) {
      await exposuresStore.createExposures(
        experimentId.value
      );

      await groupsStore.createAndAssignGroups(
        experimentId.value
      );

      await apiStore.reportStep({
        experimentId: experimentId.value,
        step
      });
    }
  }
}

async function handleDeleteCondition(condition) {
  if (condition.defaultCondition) {
    await Swal.fire(
      "You are attempting to delete the default condition. You must set another condition as default first."
    );
    return;
  }

  const result =
    await displayDeleteConditionDialog(
      condition.name
    );

  if (!result.isConfirmed) {
    return;
  }

  try {
    Object.keys(fieldErrors.value)
      .forEach(key =>
        handleRule(
          fieldErrors.value[key],
          condition.conditionId,
          false
        )
      );

    await conditionStore.deleteCondition(condition);
  } catch {
    await Swal.fire({
      text: "Could not delete condition.",
      icon: "error"
    });
  }
}

async function displayDeleteConditionDialog(
  conditionName
) {
  return Swal.fire({
    icon: "question",
    html: "<div id='alert-delete-condition'></div>",
    showCancelButton: true,
    confirmButtonText: "Yes, delete it",
    cancelButtonText: "No, cancel",
    cancelButtonColor: "#515961",
    didOpen: () => {
      const app = createApp(
        ConditionDeleteAlert,
        {
          singleConditionRemainsAfterDelete:
            singleConditionRemainsAfterDelete.value,
          conditionName
        }
      );

      app.mount(
        "#alert-delete-condition"
      );
    }
  });
}

function handleRule(
  fieldError,
  conditionId,
  hasError
) {
  if (hasError) {
    if (
      !fieldError.conditionIds.includes(
        conditionId
      )
    ) {
      fieldError.conditionIds.push(
        conditionId
      );
    }
  } else {
    const idx =
      fieldError.conditionIds.indexOf(
        conditionId
      );

    if (idx !== -1) {
      fieldError.conditionIds.splice(idx, 1);
    }
  }
}

function duplicateRule(condition) {
  handleRule(
    fieldErrors.value.duplicateName,
    condition.conditionId,
    conditions.value.some(
      c =>
        c.conditionId !==
          condition.conditionId &&
        condition.name &&
        c.name &&
        c.name
          .replace(/\s\s+/g, " ")
          .toLowerCase()
          .trim() ===
          condition.name
            .replace(/\s\s+/g, " ")
            .toLowerCase()
            .trim()
    )
  );

  return !fieldErrors.value.duplicateName.conditionIds.includes(
    condition.conditionId
  );
}

function requiredRule(condition) {
  handleRule(
    fieldErrors.value.requiredName,
    condition.conditionId,
    !(
      condition.name &&
      condition.name
        .replace(/\s\s+/g, " ")
        .trim()
    )
  );

  return !fieldErrors.value.requiredName.conditionIds.includes(
    condition.conditionId
  );
}

function maxLengthRule(condition) {
  handleRule(
    fieldErrors.value.maxLengthName,
    condition.conditionId,
    (condition.name || "").length > 255
  );

  return !fieldErrors.value.maxLengthName.conditionIds.includes(
    condition.conditionId
  );
}

function calculateFieldErrors() {
  return Object.keys(
    fieldErrors.value
  ).some(
    key =>
      fieldErrors.value[key]
        .conditionIds.length > 0
  );
}

function clearFieldErrors() {
  Object.keys(fieldErrors.value)
    .forEach(key => {
      fieldErrors.value[key].conditionIds =
        [];
    });
}

async function saveExit() {
  if (
    conditions.value.every(
      c => !(c.name && c.name.trim())
    )
  ) {
    router.push({
      name: saveExitPage.value,
      params: {
        experimentId: experimentId.value
      }
    });

    return;
  }

  if (hasFieldErrors.value) {
    await Swal.fire(
      `There was an error saving your conditions. ${errorMessage.value}`
    );

    conditionsForm.value?.validate();

    return;
  }

  saveConditions(
    saveExitPage.value,
    experiment.value.exposureType !==
      "NOSET" &&
      experiment.value.distributionType !==
        "NOSET"
  );
}

async function ensureDefaultConditions() {
  if (
    !experiment.value.conditions ||
    experiment.value.conditions.length ===
      0
  ) {
    await conditionStore.createDefaultConditions(
      route.params.experimentId
    );
  }
}

onMounted(async () => {
  await ensureDefaultConditions();
  conditionsForm.value?.validate();
});

onBeforeRouteUpdate(async () => {
  await ensureDefaultConditions();
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
.add_condition {
  text-transform: unset !important;
}
.delete_condition {
  border-radius: 4px;
  width: 100%;
  height: 56px;
}
.swal2-styled {
  &.swal2-cancel {
    background-color: map.get($swal, "cancel");
  }
}
</style>
