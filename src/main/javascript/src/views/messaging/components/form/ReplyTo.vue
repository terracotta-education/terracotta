<template>
  <div
    v-if="isLoaded"
    class="reply-to mb-5 pb-2"
  >
    <h4>Reply-to addresses</h4>

    <p class="text-medium-emphasis pb-0">
      Decide who will receive email replies
    </p>

    <v-combobox
      v-model="replyToSelection"
      v-model:search="newEmail"
      :items="replyTo"
      :readonly="readOnly"
      :persistent-hint="true"
      :hide-no-data="true"

      :error-messages="errorMessages"
      @keydown.enter.prevent="updateReplyTo"
      @blur="updateReplyTo"
      item-title="email"
      item-value="order"
      label="Email addresses"
      hint="Type an email address and press enter to add it"
      variant="outlined"
      menu-icon=""
      return-object
      hide-selected
      multiple
      chips
    >
      <template #chip="{ props: chipProps, item }">
        <v-chip
          v-bind="chipProps"
          :closable="allowRemoveReplyTo"
          @click:close="removeReplyTo"
        >
          {{ item.email }}
        </v-chip>
      </template>
    </v-combobox>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted
} from "vue";

import Swal from "sweetalert2";
import { initValidations } from "@/helpers/messaging/validation.js";

defineOptions({
  name: "ReplyTo"
});

const props = defineProps({
  replyTos: {
    type: Array,
    default: () => []
  },
  required: {
    type: Boolean,
    default: true
  },
  readOnly: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "updated"
]);


const replyTo = ref([]);
const newEmail = ref(null);
const replyToSelection = ref([]);
const validationErrors = ref(null);
const hasErrors = ref(false);
const isLoaded = ref(false);

const allowRemoveReplyTo = computed(() => {
  return replyTo.value.length > 1;
});

const errorMessages = computed(() => {
  const errors = [];

  if (!validationErrors.value) {
    return errors;
  }

  if (validationErrors.value.invalid) {
    errors.push(validationErrors.value.invalid);
  }

  if (validationErrors.value.required) {
    errors.push(validationErrors.value.required);
  }

  return errors;
});

const validateEmail = email => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

const resetValidation = () => {
  validationErrors.value = initValidations().container.replyTo;
  hasErrors.value = false;
};

const emitUpdated = () => {
  emit("updated", replyTo.value);
};

const setInvalidError = message => {
  validationErrors.value.invalid = message;
  hasErrors.value = true;
};

const showInvalidEmailAlert = email => {
  Swal.fire({
    title: "Invalid email",
    html: `The email you entered is not valid: <b>${email}</b>`,
    icon: "error",
    confirmButtonText: "Ok"
  });
};

const syncSelectionToReplyTo = () => {
  replyTo.value = replyToSelection.value
    ? [...replyToSelection.value]
    : [];

  emitUpdated();
};

const createReplyTo = email => {
  return {
    id: null,
    containerConfigurationId: null,
    email,
    messageConfigurationId: null,
    order: replyToSelection.value.length
  };
};

const updateReplyTo = async () => {
  // Vuetify 3 auto-adds the raw search string to the model on Enter before this
  // handler fires; remove those string entries so our object-based logic takes over.
  replyToSelection.value = replyToSelection.value.filter(
    item => typeof item === "object" && item !== null
  );

  if (
    !(replyToSelection.value.length || newEmail.value) &&
    props.required
  ) {
    replyTo.value = [];
    replyToSelection.value = [];
    emitUpdated();

    Swal.fire({
      title: "Reply-to email is required",
      html: "Please add a reply-to email address.",
      icon: "error",
      confirmButtonText: "Ok"
    });

    setInvalidError("A reply-to email is required.");

    return false;
  }

  if (newEmail.value) {
    const email =
      typeof newEmail.value === "string"
        ? newEmail.value
        : newEmail.value.email;

    if (!validateEmail(email)) {
      showInvalidEmailAlert(email);
      setInvalidError(
        `The email you entered is not valid: "${email}"`
      );

      syncSelectionToReplyTo();
      newEmail.value = null;

      return false;
    }

    const exists = replyToSelection.value.some(
      reply => reply.email === email
    );

    if (!exists) {
      replyToSelection.value.push(
        createReplyTo(email)
      );
    }
  }

  syncSelectionToReplyTo();
  resetValidation();
  newEmail.value = null;

  return true;
};

const removeReplyTo = () => {
  // v-combobox already removes the chip's entry from replyToSelection (our
  // v-model) before this handler runs, so there's nothing left to find and
  // splice here - just sync the already-updated selection through.
  syncSelectionToReplyTo();
};

const isValid = () => {
  return !hasErrors.value;
};

const initializeReplyTo = () => {
  replyTo.value = (props.replyTos ?? []).map((reply, index) => ({
    ...reply,
    order: index
  }));

  replyToSelection.value = [...replyTo.value];
};

onMounted(() => {
  initializeReplyTo();
  resetValidation();
  isLoaded.value = true;
});

defineExpose({
  updateReplyTo,
  isValid
});
</script>

<style scoped>
.reply-to {
  :deep(.v-input__append) {
    display: none;
  }
}
</style>
