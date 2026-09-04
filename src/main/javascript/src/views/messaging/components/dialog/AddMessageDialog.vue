<template>
  <div v-if="isSingleConditionExperiment">
    <v-btn
      v-if="hasExisting"
      color="primary"
      elevation="0"
      @click="handleVersionSelection('SINGLE')"
    >
      Add message
    </v-btn>

    <v-btn
      v-else
      class="btn-create-first-message"
      elevation="0"
      @click="handleVersionSelection('SINGLE')"
    >
      Create message
    </v-btn>
  </div>

  <div v-else>
    <v-menu
      v-model="addMessageDialogOpen"
      :close-on-content-click="false"
      :open-on-click="true"
      :open-on-hover="false"
      content-class="add-message-dialog"
      transition="scale-transition"
      location="bottom end"
    >
      <template #activator="{ props: menuProps }">
        <v-btn
          v-if="hasExisting"
          v-bind="menuProps"
          :disabled="disableAddMessageButton"
          color="primary"
          elevation="0"
        >
          Add Message
        </v-btn>

        <v-btn
          v-else
          v-bind="menuProps"
          class="btn-create-first-message ml-4"
          elevation="0"
        >
          Create Message
        </v-btn>
      </template>

      <div class="add-message-dialog-content">
        <div class="add-message-version-option">
          <v-btn
            role="menuitem"
            color="primary"
            elevation="0"
            tabindex="0"
            @click="handleVersionSelection('MULTIPLE')"
            @keyup.enter="handleVersionSelection('MULTIPLE')"
            @keyup.space="handleVersionSelection('MULTIPLE')"
          >
            With Different Versions
          </v-btn>

          <p>
            Create <u>multiple</u> treatments so your students receive
            different messages.
          </p>
        </div>

        <div class="add-message-version-option">
          <v-btn
            role="menuitem"
            color="primary"
            elevation="0"
            tabindex="0"
            @click="handleVersionSelection('SINGLE')"
            @keyup.enter="handleVersionSelection('SINGLE')"
            @keyup.space="handleVersionSelection('SINGLE')"
          >
            With Only One Version
          </v-btn>

          <p>
            Create <u>one</u> treatment so all students receive the same
            message.
          </p>
        </div>
      </div>
    </v-menu>
  </div>
</template>

<script setup>
import {
  ref,
  watch
} from "vue";

defineOptions({
  name: "AddMessageDialog"
});

defineProps({
  hasExistingMessage: {
    type: Boolean,
    default: false
  },
  hasExisting: {
    type: Boolean,
    default: false
  },
  isSingleConditionExperiment: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "add"
]);

const addMessageDialogOpen = ref(false);
const disableAddMessageButton = ref(false);

watch(addMessageDialogOpen, value => {
  disableAddMessageButton.value = value;
});

const handleVersionSelection = version => {
  emit("add", version);
  addMessageDialogOpen.value = false;
};
</script>

<style lang="scss" scoped>
.btn-create-first-message {
  border-radius: 24px;
  width: fit-content;
  min-height: 48px;
  background-color: white !important;
  border: 1px solid;
}

:deep(.add-message-dialog) {
  width: 350px;
  background-color: white;
  padding: 5px 5px 0 5px;
  border: thin solid lightgrey;
}

.add-message-dialog-content {
  > div.add-message-version-option {
    border: thin solid lightgrey;
    border-radius: 5px;
    padding: 5px;
    margin-bottom: 5px;
    text-align: center;

    > p {
      margin-bottom: 0 !important;
      padding-bottom: 0 !important;
      text-align: left;
    }
  }
}
</style>
