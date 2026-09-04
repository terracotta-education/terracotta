<template>
  <div class="editor-sub-menu">
    <FileList
      v-if="showAttachments"
      :experiment-id="experimentId"
      :exposure-id="exposureId"
      :container-id="containerId"
      :message-id="messageId"
      :content-id="contentId"
      :read-only="readOnly"
      :class="{ 'file-list': !readOnly }"
    />

    <v-menu
      v-if="!readOnly && showPipedText && pipedTextItems.length > 0"
      :disabled="pipedTextItems.length === 0"
      class="piped-text-menu"
      open-on-hover
      location="top"
    >
      <template #activator="{ props: menuProps }">
        <v-btn
          v-bind="menuProps"
          aria-label="add piped text"
          color="primary"
          class="px-0"
          variant="text"
        >
          INSERT PIPED TEXT
        </v-btn>
      </template>

      <v-list density="compact">
        <v-list-item
          v-for="item in pipedTextItems"
          :key="item.id"
          aria-label="select piped text"
          @click="insertPipedText(item)"
        >
          <v-list-item-title class="piped-text-menu-item">
            <div class="piped-text-item">
              {{ truncate(item.key) }}
            </div>
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>

    <v-btn
      v-if="showConditionalText && !hasConditionalTexts && !readOnly"
      :disabled="!hasMessageRuleAssignments"
      aria-label="add conditional text"
      color="primary"
      class="px-0"
      variant="text"
      @click="addConditionalText"
    >
      INSERT CONDITIONAL TEXT
    </v-btn>

    <v-menu
      v-if="showConditionalText && hasConditionalTexts && !readOnly"
      class="conditional-text-menu"
      open-on-hover
      location="top"
    >
      <template #activator="{ props: menuProps }">
        <v-btn
          v-bind="menuProps"
          :disabled="!hasMessageRuleAssignments"
          color="primary"
          class="px-0"
          variant="text"
        >
          INSERT CONDITIONAL TEXT
        </v-btn>
      </template>

      <v-list density="compact">
        <v-list-item
          v-for="conditionalText in conditionalTexts"
          :key="conditionalText.id"
          aria-label="select conditional text"
        >
          <v-list-item-title class="conditional-text-menu-item">
            <button
              type="button"
              class="conditional-text-item"
              @click="insertConditionalText(conditionalText)"
            >
              {{ truncate(conditionalText.label) }}
            </button>

            <button
              type="button"
              class="conditional-text-item-edit px-0"
              @click="editConditionalText(conditionalText)"
            >
              edit
            </button>
          </v-list-item-title>
        </v-list-item>

        <v-list-item
          :class="{ 'no-border': !conditionalTexts.length }"
          class="conditional-text-add-new"
          @click="addConditionalText"
        >
          <v-list-item-title>
            Add new conditional text
          </v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>

<script setup>
import { computed } from "vue";

import FileList from "@/views/messaging/components/attachments/FileList.vue";

import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";
import { message as messagingMessageModule } from "@/store/messaging/message.module";

defineOptions({
  name: "EditorSubMenu"
});

defineProps({
  experimentId: {
    type: Number,
    required: true
  },
  exposureId: {
    type: String,
    required: true
  },
  containerId: {
    type: String,
    required: true
  },
  messageId: {
    type: String,
    required: true
  },
  contentId: {
    type: String,
    required: true
  },
  maxRuleCount: {
    type: Number,
    default: 8
  },
  validatedErrors: {
    type: Object,
    default: null
  },
  showAttachments: {
    type: Boolean,
    default: true
  },
  showPipedText: {
    type: Boolean,
    default: true
  },
  showConditionalText: {
    type: Boolean,
    default: true
  },
  readOnly: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits([
  "insertPipedText",
  "insertConditionalText",
  "addConditionalText",
  "editConditionalText"
]);

const messagingConditionalTextStore = messagingConditionalTextModule();
const messagingMessageStore = messagingMessageModule();

const allConditionalTexts = computed(() => {
  return messagingConditionalTextStore.messageConditionalTexts || [];
});

const allMessageRuleAssignments = computed(() => {
  return messagingMessageStore.assignments || [];
});

const pipedText = computed(() => {
  return messagingMessageStore.pipedText;
});

const conditionalTexts = computed(() => {
  return allConditionalTexts.value || [];
});

const hasConditionalTexts = computed(() => {
  return conditionalTexts.value.length > 0;
});

const hasMessageRuleAssignments = computed(() => {
  return allMessageRuleAssignments.value.length > 0;
});

const pipedTextItems = computed(() => {
  return pipedText.value?.items || [];
});

const truncate = (
  value,
  length = 30
) => {
  if (!value) {
    return "";
  }

  return value.length > length
    ? `${value.slice(0, length - 1)}... `
    : value;
};

const insertPipedText = item => {
  emit("insertPipedText", item);
};

const insertConditionalText = conditionalText => {
  emit("insertConditionalText", conditionalText);
};

const addConditionalText = () => {
  emit("addConditionalText");
};

const editConditionalText = conditionalText => {
  emit("editConditionalText", conditionalText.id);
};
</script>

<style scoped>
.editor-sub-menu {
  display: flex;
  align-content: start;
  min-width: 100%;
  max-width: 100%;
  margin-top: -26px;
  border: 1px solid #9e9e9e;
  border-top: none;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;

  > button {
    margin-left: 10px;
    max-width: fit-content;
  }

  & .file-list {
    border-right: 1px solid #9e9e9e;
  }
}

.conditional-text-menu-item,
.piped-text-menu-item {
  display: flex;
  flex-direction: row;
  justify-content: space-between;

  & .piped-text-item,
  & .conditional-text-item,
  & .conditional-text-item-edit {
    color: rgba(0, 0, 0, 0.87);
    text-decoration: none;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }

  & .conditional-text-item,
  & .piped-text-item {
    min-width: 80%;
    max-width: 80%;
  }

  & .conditional-text-item-edit {
    min-width: 15%;
    max-width: 15%;
  }
}

.conditional-text-item,
.conditional-text-item-edit {
  border: 0;
  background: transparent;
  text-align: left;
}

.conditional-text-add-new {
  border-top: 1px solid #9e9e9e;
  color: rgba(0, 0, 0, 0.87);
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.no-border {
  border: none !important;
}
</style>
