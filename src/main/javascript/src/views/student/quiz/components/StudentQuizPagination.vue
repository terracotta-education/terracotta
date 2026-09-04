<template>
  <v-btn
    v-if="showBackButton"
    :disabled="disableBackButton"
    elevation="0"
    color="primary"
    class="mt-4 mr-2"
    type="button"
    @click.prevent="$emit('back')"
  >
    Back
  </v-btn>

  <v-btn
    v-if="showNextButton"
    :disabled="disableNextButton"
    elevation="0"
    color="primary"
    class="mt-4"
    type="button"
    @click.prevent="$emit('next')"
  >
    Next
  </v-btn>

  <v-btn
    v-if="!preview && showSubmitButton"
    :disabled="disableSubmitButton"
    elevation="0"
    color="primary"
    class="mt-4"
    type="submit"
  >
    Submit
  </v-btn>

  <v-btn
    v-if="preview && showSubmitButton"
    :disabled="disableSubmitButton"
    :href="`/preview/experiments/${experimentId}/conditions/${conditionId}/treatments/${treatmentId}/complete?ownerId=${ownerId}`"
    elevation="0"
    color="primary"
    class="mt-4"
  >
    Submit
  </v-btn>
</template>

<script setup>
defineProps({
  preview: { type: Boolean, default: false },
  showBackButton: { type: Boolean, default: false },
  disableBackButton: { type: Boolean, default: false },
  showNextButton: { type: Boolean, default: false },
  disableNextButton: { type: Boolean, default: false },
  showSubmitButton: { type: Boolean, default: false },
  disableSubmitButton: { type: Boolean, default: false },
  experimentId: { type: [String, Number], required: true },
  conditionId: { type: [String, Number], default: null },
  treatmentId: { type: [String, Number], default: null },
  ownerId: { type: [String, Number], default: null }
});

defineEmits(["back", "next"]);
</script>
