<template>
<div
  class="file-list"
>
  <v-menu
    v-model="menu"
    :close-on-content-click="false"
    :nudge-width="200"
    offset-x
  >
    <template v-slot:activator="{ on, attrs }">
      <v-btn
        v-bind="attrs"
        v-on="on"
        text
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

    <v-card
      class="file-list-menu"
    >
      <v-list>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title
              class="mb-3"
            >
              Select files to attach
            </v-list-item-title>
            <v-list-item-subtitle
              class="file-list-subtitle"
            >
              <div>
                Please choose the file(s) you want to attach from the list below. If your file isn't there, please add it to
                <a
                  :href="myFilesUrl"
                  target="_blank"
                >
                  My Files > conversation attachments
                </a>
                and try again.
              </div>
              <v-btn
                :disabled="isRefreshingFiles || readOnly"
                @click="refreshFiles"
                color="primary"
                class="mt-3 px-0"
                text
              >
                Refresh File List
              </v-btn>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <v-divider />
      <v-list>
        <v-list-item
          v-for="(file) in files"
          :key="file.lmsId"
        >
          <v-list-item-action>
            <v-checkbox
              v-model="selectedFiles"
              :label="label(file)"
              :value="file.lmsId"
              :disabled="readOnly"
            ></v-checkbox>
          </v-list-item-action>
        </v-list-item>
      </v-list>
      <v-card-actions>
        <v-spacer />
        <v-btn
          color="primary"
          @click="menu = false"
          text
        >
          Done
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-menu>
</div>
</template>

<script>
import { mapGetters, mapActions, mapMutations } from "vuex";

export default {
  props: {
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
  },
  data: () => ({
    menu: false,
    isRefreshingFiles: false
  }),
  computed: {
    ...mapGetters({
      all: "messagingContentAttachment/get",
      allMessageContainers: "messagingMessageContainer/messageContainers"
    }),
    container() {
      return this.allMessageContainers.find(container => container.id === this.containerId);
    },
    message() {
      return this.container.messages.find(message => message.id === this.messageId);
    },
    content() {
      return this.message.content;
    },
    attachments: {
      get() {
        return this.content.attachments || [];
      },
      set(newAttachments) {
        this.content.attachments = newAttachments;
      }
    },
    selectedFiles: {
      get() {
        return this.attachments.map(file => file.lmsId);
      },
      set(newSelectedFiles) {
        this.attachments = this.files.filter(file => newSelectedFiles.includes(file.lmsId));
      }
    },
    files() {
      if (!this.all || !this.all.length) {
        return this.attachments;
      }

      return [...this.attachments, ...this.all.filter(file => !this.attachments.some(att => att.lmsId === file.lmsId))];
    },
    buttonColor() {
      return this.readOnly ? "disabled" : "primary";
    },
    myFilesUrl() {
      return this.container.myFilesUrl || "";
    }
  },
  methods: {
    ...mapActions({
      get: "messagingContentAttachment/getAll"
    }),
    ...mapMutations({
      set: "messagingContentAttachment/set"
    }),
    label(file) {
      return file.displayName || file.filename || "Untitled";
    },
    async refreshFiles() {
      this.isRefreshingFiles = true;
      await this.get(
        [
          this.experimentId,
          this.exposureId,
          this.containerId,
          this.messageId,
          this.contentId
        ]
      );
      this.isRefreshingFiles = false;
    }
  },
  async mounted() {
    await this.get(
      [
        this.experimentId,
        this.exposureId,
        this.containerId,
        this.messageId,
        this.contentId
      ]
    );
  }
}
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
