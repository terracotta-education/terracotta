<template>
  <div class="file-list">
    <v-menu
      v-model="menu"
      :close-on-content-click="false"
      width="500"
      location="end"
    >
      <template #activator="{ props: menuProps }">
        <v-btn
          v-bind="menuProps"
          aria-label="add attachments to message"
          variant="text"
        >
          <v-icon>mdi-paperclip</v-icon>

          <span
            v-if="selectedFiles.length"
            class="ml-2"
          >
            ({{ selectedFiles.length }})
          </span>
        </v-btn>
      </template>

      <v-card class="file-list-menu">
        <v-list>
          <v-list-item>
            <v-list-item-title class="mb-3">
              Select files to attach
            </v-list-item-title>

            <v-list-item-subtitle class="file-list-subtitle">
              <div>
                Please choose the file(s) you want to attach from the list below.
                If your file isn't there, please add it to
                <a
                  :href="myFilesUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  My Files &gt; conversation attachments
                </a>
                and try again.
              </div>

              <v-btn
                :disabled="isRefreshingFiles || readOnly"
                color="primary"
                class="mt-3 px-0"
                variant="text"
                @click="refreshFiles"
              >
                Refresh File List
              </v-btn>
            </v-list-item-subtitle>
          </v-list-item>
        </v-list>

        <v-divider />

        <v-list>
          <v-list-item
            v-for="file in files"
            :key="file.lmsId"
          >
            <template #prepend>
              <v-checkbox
                v-model="selectedFiles"
                :label="label(file)"
                :value="file.lmsId"
                :disabled="readOnly"
                hide-details
              />
            </template>
          </v-list-item>
        </v-list>

        <v-card-actions>
          <v-spacer />

          <v-btn
            color="primary"
            variant="text"
            @click="menu = false"
          >
            Done
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-menu>
  </div>
</template>

<script setup>
import {
  ref,
  computed,
  onMounted
} from "vue";

import { attachment as messagingContentAttachmentModule } from "@/store/messaging/attachment.module";
import { container as messagingMessageContainerModule } from "@/store/messaging/container.module";

defineOptions({
  name: "FileList"
});

const props = defineProps({
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
  readOnly: {
    type: Boolean,
    default: false
  }
});

const messagingContentAttachmentStore = messagingContentAttachmentModule();
const messagingMessageContainerStore = messagingMessageContainerModule();

const menu = ref(false);
const isRefreshingFiles = ref(false);

const all = computed(() => {
  return messagingContentAttachmentStore.attachments || [];
});

const allMessageContainers = computed(() => {
  return messagingMessageContainerStore.messageContainers || [];
});

const container = computed(() => {
  return allMessageContainers.value.find(
    currentContainer => currentContainer.id === props.containerId
  );
});

const message = computed(() => {
  return container.value?.messages.find(
    currentMessage => currentMessage.id === props.messageId
  );
});

const content = computed(() => {
  return message.value?.content || {};
});

const attachments = computed({
  get() {
    return content.value.attachments || [];
  },

  set(value) {
    content.value.attachments = value;
  }
});

const selectedFiles = computed({
  get() {
    return attachments.value.map(file => file.lmsId);
  },

  set(value) {
    attachments.value = files.value.filter(file =>
      value.includes(file.lmsId)
    );
  }
});

const files = computed(() => {
  if (!all.value.length) {
    return attachments.value;
  }

  return [
    ...attachments.value,
    ...all.value.filter(file => {
      return !attachments.value.some(
        attachment => attachment.lmsId === file.lmsId
      );
    })
  ];
});

const myFilesUrl = computed(() => {
  return container.value?.myFilesUrl || "";
});

const label = file => {
  return file.displayName || file.filename || "Untitled";
};

const refreshFiles = async () => {
  isRefreshingFiles.value = true;

  await messagingContentAttachmentStore.getAll([
    props.experimentId,
    props.exposureId,
    props.containerId,
    props.messageId,
    props.contentId
  ]);

  isRefreshingFiles.value = false;
};

onMounted(async () => {
  await refreshFiles();
});
</script>

<style scoped>
.file-list {
  > button {
    border: none;
  }
}

.file-list-menu {
  max-width: 500px;

  & .file-list-subtitle {
    white-space: normal;
    flex-direction: column;
    align-items: center;
  }
}
</style>
