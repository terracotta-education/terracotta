<template>
<div
  class="editor-sub-menu"
>
  <file-list
    v-if="showAttachments"
    :experimentId="experimentId"
    :exposureId="exposureId"
    :containerId="containerId"
    :messageId="messageId"
    :contentId="contentId"
    :readOnly="readOnly"
    :class="{ 'file-list': !readOnly }"
  />
  <v-menu
    v-if="!readonly && showPipedText && pipedTextItems.length > 0"
    :disabled="pipedTextItems.length === 0"
    class="piped-text-menu"
    open-on-hover
    offset-y
    top
  >
    <template
      v-slot:activator="{ on, attrs }"
    >
      <v-btn
        v-if="!readOnly"
        v-bind="attrs"
        v-on="on"
        color="primary"
        class="px-0"
        text
      >
        INSERT PIPED TEXT
      </v-btn>
    </template>
    <v-list
      dense
    >
      <v-list-item
        v-for="item in pipedTextItems"
        :key="item.id"
        aria-label="select piped text"
      >
        <v-list-item-content>
          <v-list-item-title
            class="piped-text-menu-item"
          >
            <div
              @click="insertPipedText(item)"
              class="piped-text-item"
            >
                {{ truncate(item.key) }}
            </div>
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </v-list>
  </v-menu>
  <v-btn
    v-if="showConditionalText && !hasConditionalTexts"
    :disabled="!hasMessageRuleAssignments"
    @click="addConditionalText"
    color="primary"
    class="px-0"
    text
  >
    INSERT CONDITIONAL TEXT
  </v-btn>
  <v-menu
    v-if="showConditionalText && hasConditionalTexts"
    class="conditional-text-menu"
    open-on-hover
    offset-y
    top
  >
    <template
      v-slot:activator="{ on, attrs }"
    >
      <v-btn
        v-if="!readOnly"
        :disabled="!hasMessageRuleAssignments"
        v-bind="attrs"
        v-on="on"
        color="primary"
        class="px-0"
        text
      >
        INSERT CONDITIONAL TEXT
      </v-btn>
    </template>
    <v-list
      dense
    >
      <v-list-item
        v-for="conditionalText in conditionalTexts"
        :key="conditionalText.id"
        aria-label="select conditional text"
      >
        <v-list-item-content>
          <v-list-item-title
            class="conditional-text-menu-item"
          >
            <div
              @click="insertConditionalText(conditionalText)"
              class="conditional-text-item"
            >
                {{ truncate(conditionalText.label) }}
            </div>
            <div
              @click="editConditionalText(conditionalText)"
              class="conditional-text-item-edit px-0"
            >
                edit
            </div>
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
      <v-list-item
        v-if="!readonly"
        :class="{'no-border': !conditionalTexts.length}"
        class="conditional-text-add-new"
      >
        <v-list-item-title>
          <a
            @click="addConditionalText"
            class="px-0"
          >
            Add new conditional text
          </a>
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</div>
</template>

<script>
import { mapGetters } from "vuex";
import FileList from "@/views/messaging/components/attachments/FileList.vue";

export default {
  components: {
    FileList
  },
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
  },
  computed: {
    ...mapGetters({
      allConditionalTexts: "messagingConditionalText/messageConditionalTexts",
      allMessageRuleAssignments: "messagingMessage/assignments",
      pipedText: "messagingMessage/pipedText"
    }),
    conditionalTexts() {
      return this.allConditionalTexts || [];
    },
    hasConditionalTexts() {
      return this.conditionalTexts.length > 0;
    },
    hasMessageRuleAssignments() {
      return this.allMessageRuleAssignments.length > 0;
    },
    pipedTextItems() {
      return this.pipedText?.items || [];
    }
  },
  methods: {
    truncate(str, n = 30) {
      if (!str) {
        return "";
      }

      return (str.length > n) ? str.slice(0, n-1) + "... " : str;
    },
    insertPipedText(item) {
      this.$emit("insertPipedText", item);
    },
    insertConditionalText(conditionalText) {
      this.$emit("insertConditionalText", conditionalText);
    },
    addConditionalText() {
      this.$emit("addConditionalText");
    },
    editConditionalText(conditionalText) {
      this.$emit("editConditionalText", conditionalText.id);
    }
  }
}
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
    color: rgba(0, 0, 0, .87);
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
.conditional-text-add-new {
  border-top: 1px solid #9e9e9e;
  & a {
    color: rgba(0, 0, 0, .87);
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }
}
.no-border {
  border: none !important;
}
</style>
