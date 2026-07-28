<template>
  <div
    class="treatment-row-content d-flex align-center justify-space-between"
  >
    <div
      class="treatment-info-group ml-8"
    >
      <v-icon class="mr-1 component-icon">
        {{ rowTreatmentsIcon }}
      </v-icon>
      <ToolTip
        v-if="showTreatmentRowTooltip"
        :content="treatmentRowTooltipText"
        :ref="`tooltip-${row.assignmentId}-${treatment.treatmentId}`"
        aria-label="treatment explanation tooltip"
        icon="mdi-information-outline"
        alignment="top"
        activator-type="icon"
        activator-class="icon-treatment-incomplete"
      />
      <span :class="treatmentRowClass">
        Treatment
      </span>
      <v-chip
        v-if="showConditionChip"
        :color="conditionColorMapping[conditionName]"
        variant="flat"
        density="compact"
        label
      >
        {{ conditionName }}
      </v-chip>
    </div>
    <div class="treatment-btn-group">
      <v-btn
        variant="text"
        class="btn-treatment-edit"
        @click="$emit('edit-treatment', { row, treatment })"
      >
        <v-icon>{{ editTreatmentIcon }}</v-icon>
        <span class="btn-edit">{{ editTreatmentText }}</span>
      </v-btn>

      <v-btn
        v-if="isIntegrationAssignment && !displayTreatmentMenu"
        :href="integrationsPreviewLaunchUrl(treatment.assessmentDto.integrationPreviewUrl)"
        :disabled="!treatment.assessmentDto.questions.length || !treatment.assessmentDto.integrationUrlValid"
        target="_blank"
        variant="text"
      >
        <v-icon>mdi-eye-outline</v-icon>
        <span class="treatment-btn">Preview</span>
      </v-btn>

      <v-btn
        v-if="!isMessage && !treatment.assessmentDto.integration"
        :disabled="!treatment.assessmentDto.questions.length"
        variant="text"
        @click="$emit('preview-treatment', treatment)"
      >
        <v-icon>mdi-eye-outline</v-icon>
        <span class="treatment-btn">Preview</span>
      </v-btn>

      <v-menu
        v-if="isIntegrationAssignment && displayTreatmentMenu"
        :disabled="!treatment.assessmentDto.questions.length || !treatment.assessmentDto.integrationUrlValid"
        location="start"
      >
        <template #activator="{ props: menuProps }">
          <v-btn
            v-bind="menuProps"
            aria-label="treatment actions"
            icon="mdi-dots-horizontal"
            variant="text"
          />
        </template>

        <v-list>
          <v-list-item aria-label="preview integration">
            <v-list-item-title>
              <v-icon>mdi-eye-outline</v-icon>
              <span class="treatment-btn">
                <a
                  :href="integrationsPreviewLaunchUrl(treatment.assessmentDto.integrationPreviewUrl)"
                  target="_blank"
                  class="integration-preview-link"
                >
                  Preview
                </a>
              </span>
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { message as messageStatus } from "@/helpers/messaging/status.js";
import ToolTip from "@/components/ToolTip.vue";

const props = defineProps({
  row: {
    type: Object,
    required: true
  },
  treatment: {
    type: Object,
    required: true
  },
  exposure: {
    type: Object,
    required: true
  },
  conditions: {
    type: Array,
    required: true
  },
  conditionColorMapping: {
    type: Object,
    required: true
  },
  singleConditionExperiment: {
    type: Boolean,
    default: false
  },
  displayTreatmentMenu: {
    type: Boolean,
    default: false
  }
});

defineEmits(["edit-treatment", "preview-treatment"]);

const rowType = {
  assignment: "assignment",
  message: "message"
};

const treatmentIcon = {
  integration: "mdi-application-brackets-outline",
  assignment: "mdi-wrench-outline",
  file: "mdi-file-outline",
  message: "mdi-message-text-outline"
};

const isMessage = computed(() => props.row.type === rowType.message);
const isIntegrationAssignment = computed(() => {
  return props.row.type === rowType.assignment && props.treatment.assessmentDto.integration;
});

const rowTreatmentsIcon = computed(() => {
  if (props.row.type === rowType.assignment) {
    return props.treatment.assessmentDto.integration
      ? treatmentIcon.integration
      : treatmentIcon.assignment;
  }

  if (props.row.type === rowType.message) {
    return treatmentIcon.message;
  }

  return "";
});

const conditionForTreatment = computed(() => {
  return props.exposure.groupConditionList.find(
    condition => condition.conditionId === props.treatment.conditionId
  );
});

const conditionName = computed(() => conditionForTreatment.value?.conditionName || "");
const showConditionChip = computed(() => {
  return !props.singleConditionExperiment &&
    props.row.treatments.length === props.conditions?.length;
});

const showTreatmentRowTooltip = computed(() => {
  if (props.row.type === rowType.assignment) {
    if (props.treatment.assessmentDto.integration && !props.treatment.assessmentDto.integrationUrlValid) {
      return true;
    }

    return !(props.treatment.assessmentDto && props.treatment.assessmentDto.questions.length);
  }

  if (props.row.type === rowType.message) {
    return ![
      messageStatus.ready,
      messageStatus.disabled,
      messageStatus.sent
    ].includes(props.treatment.configuration.status);
  }

  return false;
});

const treatmentRowTooltipText = computed(() => {
  if (props.row.type === rowType.assignment) {
    if (props.treatment.assessmentDto.integration && !props.treatment.assessmentDto.integrationUrlValid) {
      return "Error rendering content. Please check the URL.";
    }

    return "Please add content to this treatment.";
  }

  if (props.row.type === rowType.message) {
    return "Please create a message for this treatment.";
  }

  return "";
});

const treatmentRowClass = computed(() => {
  return showTreatmentRowTooltip.value
    ? "label-treatment-incomplete"
    : "label-treatment-complete";
});

const editTreatmentIcon = computed(() => {
  if (props.row.type === rowType.assignment) {
    return "mdi-pencil";
  }

  if (props.row.type === rowType.message) {
    return ![
      messageStatus.queued,
      messageStatus.processing,
      messageStatus.sent,
      messageStatus.deleted
    ].includes(props.treatment.configuration.status)
      ? "mdi-pencil"
      : "mdi-eye";
  }

  return "";
});

const editTreatmentText = computed(() => {
  if (props.row.type === rowType.assignment) {
    return "Edit";
  }

  if (props.row.type === rowType.message) {
    return ![
      messageStatus.queued,
      messageStatus.processing,
      messageStatus.sent,
      messageStatus.deleted
    ].includes(props.treatment.configuration.status)
      ? "Edit"
      : "View";
  }

  return "";
});

const integrationsPreviewLaunchUrl = (url = "http://localhost") => {
  return `/integrations/preview?url=${btoa(url)}`;
};
</script>
