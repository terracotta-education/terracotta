<template>
  <div class="type-radio mb-0">
    <h4>{{ label }}</h4>

    <v-radio-group
      v-model="selectedType"
      :disabled="readOnly"
      :hide-details="!validationErrors"
      :error-messages="validationErrors"
      density="compact"
      @update:model-value="handleTypeAlert"
    >
      <v-radio
        v-for="messageType in types"
        :key="messageType.value"
        :value="messageType.value"
        :label="messageType.label"
        color="blue"
        class="mb-2"
      />
    </v-radio-group>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import Swal from "sweetalert2";
import { validations } from "@/helpers/messaging/validation";

defineOptions({
  name: "MessageType"
});

const props = defineProps({
  type: {
    type: String,
    default: null
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ""
  },
  validatedErrors: {
    type: Object,
    default: null
  }
});

const emit = defineEmits([
  "updated"
]);


const selectedType = ref(null);
const validationErrors = ref(null);

const types = [
  {
    value: "EMAIL",
    label: "Email"
  },
  {
    value: "CONVERSATION",
    label: "Canvas message"
  }
];

watch(
  () => props.type,
  value => {
    selectedType.value = value;
  },
  {
    immediate: true
  }
);

watch(
  () => props.validatedErrors,
  value => {
    validationErrors.value =
      value || validations.container.type;
  },
  {
    immediate: true
  }
);

const handleTypeAlert = async value => {
  if (
    value === "CONVERSATION" &&
    props.type === "EMAIL"
  ) {
    const result = await Swal.fire({
      title: "Are you sure you want to switch?",
      html: "Changing from email to Canvas message will <b>erase all your formatting</b>. Do you want to proceed?",
      showCancelButton: true,
      confirmButtonText: "Yes, continue",
      cancelButtonText: "Cancel"
    });

    const typeToSet = result.isConfirmed
      ? "CONVERSATION"
      : "EMAIL";

    selectedType.value = typeToSet;
    emit("updated", typeToSet);

    return;
  }

  emit("updated", value);
};
</script>

<style scoped>
.type-radio {
  :deep(.v-selection-control-group) {
    margin-bottom: 0;
    max-width: fit-content;
    padding-right: 12px;
  }
}
</style>
