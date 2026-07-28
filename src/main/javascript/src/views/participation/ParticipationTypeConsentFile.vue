<template>
  <div>
    <h1>
      Add your study's <strong>informed consent</strong> file.
    </h1>

    <FileDropZone
      :existing-file="pdfFile"
      class="my-5"
      @update="onFileChange"
      @new-upload="onNewUpload"
      @display-file="onDisplayFile"
    />

    <div>
      <v-btn
        :disabled="!consentFileExists || uploading"
        class="mt-3 mb-6"
        color="primary"
        elevation="0"
        @click="saveConsent(nextPage)"
      >
        Next
      </v-btn>

      <Spinner v-if="uploading" />
    </div>

    <p>
      You can
      <a
        :href="icsFileUrl"
        download="Terracotta_ICS_template.docx"
      >
        download an informed consent template here.
      </a>
    </p>

    <div v-if="displayFile">
      <VuePdfEmbed :source="pdfFileDisplay" />
    </div>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  watch,
  onMounted
} from "vue";

import { useRouter } from "vue-router";
import Swal from "sweetalert2";

import FileDropZone from "@/components/FileDropZone.vue";
import Spinner from "@/components/Spinner.vue";

import VuePdfEmbed from "vue-pdf-embed";

import { configuration as configurationModule } from "@/store/configuration.module";
import { consent as consentModule } from "@/store/consent.module";
import { navigation as navigationModule } from "@/store/navigation.module";

defineOptions({
  name: "ParticipationTypeConsentFile"
});

const props = defineProps({
  experiment: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const configurationStore = configurationModule();
const consentStore = consentModule();
const navigationStore = navigationModule();

const pdfFile = ref(null);
const pdfFileDisplay = ref(null);
const uploading = ref(false);
const displayFile = ref(false);
const newUpload = ref(false);

const configurations = computed(() => configurationStore.get);

const consent = computed(() => {
  return consentStore.consent;
});

const editMode = computed(() => {
  return navigationStore.editMode;
});

const saveExitPage = computed(() => {
  return editMode.value?.callerPage?.name || "Home";
});

const nextPage = computed(() => {
  return singleConditionExperiment.value
    ? "ParticipationSummary"
    : "ParticipationDistribution";
});

const consentFileExists = computed(() => {
  return pdfFile.value != null;
});

const conditions = computed(() => {
  return props.experiment.conditions || [];
});

const singleConditionExperiment = computed(() => {
  return conditions.value.length === 1;
});

const isNewExperiment = computed(() => {
  return editMode.value === null;
});

const icsFileUrl = computed(() => {
  return configurations.value?.icsTemplateUrl;
});

watch(pdfFileDisplay, file => {
  displayFile.value = file != null;
});

const onFileChange = newFile => {
  newUpload.value = true;
  pdfFile.value = newFile;
};

const onNewUpload = isNewUpload => {
  newUpload.value = isNewUpload;
};

const onDisplayFile = showFile => {
  if (showFile) {
    getPdfForDisplay();
  } else {
    pdfFileDisplay.value = null;
  }
};

const getPdfForDisplay = async () => {
  if (newUpload.value) {
    const file = await new Promise(
      (resolve, reject) => {
        const reader = new FileReader();

        reader.readAsDataURL(pdfFile.value);

        reader.onload = () =>
          resolve(reader.result);

        reader.onerror = error =>
          reject(error);
      }
    );

    pdfFileDisplay.value = encodeURI(file);
  } else {
    pdfFileDisplay.value = pdfFile.value;
  }
};

const handleConsentFileDownload = async () => {
  const file =
    await consentStore.getConsentFile(
      props.experiment.experimentId
    );

  if (!file) {
    return;
  }

  pdfFile.value =
    "data:application/pdf;base64," +
    encodeURI(file);
};

const saveConsent = async path => {
  try {
    uploading.value = true;

    if (newUpload.value) {
      await consentStore.createConsent([
        props.experiment.experimentId,
        pdfFile.value,
        consent.value.title
      ]);
    }

    uploading.value = false;

    router.push({
      name: path,
      params: {
        experiment:
          props.experiment.experimentId
      }
    });
  } catch (error) {
    uploading.value = false;

    await Swal.fire({
      text: `Error: ${error.message}. Please try again.`,
      icon: "error"
    });
  }
};

const saveExit = () => {
  saveConsent(saveExitPage.value);
};

onMounted(() => {
  if (!isNewExperiment.value) {
    handleConsentFileDownload();
  }
});

defineExpose({
  saveExit
});
</script>

<style lang="scss" scoped>
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

$offset: 187;
$duration: 0.75s;

.spinner {
  animation: rotator $duration linear infinite;
  margin: 0 auto;
}

@keyframes rotator {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(270deg);
  }
}

.path {
  stroke-dasharray: $offset;
  stroke-dashoffset: 0;
  transform-origin: center;

  animation:
    dash $duration ease-in-out infinite,
    colors ($duration * 4) ease-in-out infinite;
}

@keyframes colors {
  0% {
    stroke: lightgrey;
  }
}

@keyframes dash {
  0% {
    stroke-dashoffset: $offset;
  }

  50% {
    stroke-dashoffset: math.div($offset, 4);
    transform: rotate(135deg);
  }

  100% {
    stroke-dashoffset: $offset;
    transform: rotate(450deg);
  }
}
</style>
