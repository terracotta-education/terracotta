<template>
  <div>
    <h1 class="my-3">
      <span class="completed-text font-weight-bold">
        You've completed section 2.
      </span>

      <br />

      Here's a summary of your experiment participation.
    </h1>

    <template v-if="experiment">
      <v-expansion-panels
        v-if="experiment.participationType"
        class="w-50 mx-auto my-0"
      >
        <v-expansion-panel
          class="py-3 mb-3"
          @click="panelExpansion"
        >
          <v-expansion-panel-title>
            <strong>Selection Method</strong>
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <p>{{ participationType }}</p>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-expansion-panels
        v-if="experiment.participationType === 'CONSENT'"
        class="w-50 mx-auto my-0"
      >
        <v-expansion-panel
          class="py-3 mb-3"
          @click="panelExpansion"
        >
          <v-expansion-panel-title>
            <strong>Component Title</strong>
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <p>{{ experiment.consent?.title }}</p>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>

      <v-expansion-panels
        v-if="experiment.participationType === 'CONSENT'"
        class="w-50 mx-auto my-0"
      >
        <v-expansion-panel
          class="py-3 mb-3"
          @click="panelExpansion"
        >
          <v-expansion-panel-title>
            <strong>Informed Consent</strong>
          </v-expansion-panel-title>

          <v-expansion-panel-text>
            <button
              v-if="!downloading && !showFile"
              class="pdfButton"
              @click="doDisplayFile"
            >
              View consent file
            </button>

            <button
              v-if="!downloading && showFile"
              class="pdfButton"
              @click="doHideFile"
            >
              Close preview
            </button>

            <Spinner v-if="downloading" />

            <div v-if="showFile">
              <VuePdfEmbed
                :source="pdfFile"
              />
            </div>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </template>

    <v-btn
      v-if="!editMode"
      class="mt-3"
      elevation="0"
      color="primary"
      @click="nextSection"
    >
      Continue to components
    </v-btn>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted
} from "vue";

import {
  useRouter,
  onBeforeRouteUpdate
} from "vue-router";

import { deleteAttributesFromElement } from "@/helpers/ui-utils.js";

import Spinner from "@/components/Spinner.vue";

import VuePdfEmbed from "vue-pdf-embed";

import { consent as consentModule } from "@/store/consent.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationSummary"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const consentStore = consentModule();
const navigationStore = navigationModule();

const pdfFile = ref(null);
const downloading = ref(false);
const showFile = ref(false);

const editMode = computed(() => {
  return navigationStore.editMode;
});

const saveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "ExperimentSummary";
});

const participationType = computed(() => {
  switch (props.experiment.participationType) {
    case "CONSENT":
      return "Invited students to consent";

    case "MANUAL":
      return "Manually determined students";

    case "AUTO":
      return "Automatically included all students";

    default:
      return "";
  }
});

watch(pdfFile, file => {
  showFile.value = file != null;
});

const doDisplayFile = async () => {
  if (!pdfFile.value) {
    downloading.value = true;

    await handleConsentFileDownload();

    downloading.value = false;
  } else {
    showFile.value = true;
  }
};

const doHideFile = () => {
  showFile.value = false;
};

const handleConsentFileDownload = async () => {
  const file = await consentStore.getConsentFile(
    props.experiment.experimentId
  );

  if (!file) {
    return;
  }

  pdfFile.value =
    `data:application/pdf;base64,${encodeURI(file)}`;
};

const nextSection = () => {
  router.push({
    name: saveExitPage.value,
    params: {
      experiment: props.experiment.experimentId
    }
  });
};

const saveExit = () => {
  nextSection();
};

const panelExpansion = () => {
  setTimeout(() => {
    deleteAttributesFromElement(
      ".v-expansion-panel",
      ["aria-expanded"]
    );
  }, 1000);
};

onMounted(() => {
  deleteAttributesFromElement(
    ".v-expansion-panel",
    ["aria-expanded"]
  );
});

/*
 * Vue 3 replacement for beforeRouteEnter
 * preserving selectionType metadata behavior
 */
onBeforeRouteUpdate((to, from) => {
  to.meta.selectionType =
    from.meta.selectionType;
});

defineExpose({
  saveExit
});
</script>

<style lang="scss">
.completed-text {
  color: map.get($green, "base") !important;
}

.v-expansion-panel {
  border: 1px solid map.get($grey, "lighter");

  button.pdfButton {
    background: none !important;
    border: none;
    padding: 0 !important;
    color: #069;
    text-decoration: underline;
    cursor: pointer;
  }

  div.vue-pdf-embed {
    width: 98%;
    margin: 20px auto;
    min-height: 300px;
    max-height: 600px;
    overflow-y: scroll;

    box-shadow:
      0 3px 1px -2px rgba(0, 0, 0, 0.2),
      0 2px 2px 0 rgba(0, 0, 0, 0.14),
      0 1px 5px 0 rgba(0, 0, 0, 0.12);
  }
}
</style>
