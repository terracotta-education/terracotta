<template>
  <div class="editor mb-6 outlined">
    <v-card
      v-if="show"
      class="editor-card"
      variant="flat"
    >
      <EditorContent
        :editor="editor"
        class="content"
      />

      <ToolBar
        v-if="showToolbar"
        :editor="editor"
        :active-items="activeItems"
      />
    </v-card>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch
} from "vue";

import {
  Editor,
  EditorContent
} from "@tiptap/vue-3";

import { findChildren } from "@tiptap/core";

import Document from "@tiptap/extension-document";
import Link from "@tiptap/extension-link";
import Mention from "@tiptap/extension-mention";
import Paragraph from "@tiptap/extension-paragraph";
import StarterKit from "@tiptap/starter-kit";
import Text from "@tiptap/extension-text";
import Underline from "@tiptap/extension-underline";
import YouTube from "@tiptap/extension-youtube";

import {
  addAttributesToObservedElement,
  addAttributesToElement
} from "@/helpers/ui-utils.js";

import ToolBar from "./ToolBar.vue";

import { conditionaltext as messagingConditionalTextModule } from "@/store/messaging/conditionaltext.module";

defineOptions({
  name: "TipTapEditor"
});

const props = defineProps({
  content: {
    type: String,
    default: ""
  },
  editorType: {
    type: String,
    default: "basic"
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  allowMentions: {
    type: Boolean,
    default: false
  },
  conditionalTextToPlace: {
    type: Object,
    default: null
  },
  pipedTextToPlace: {
    type: Object,
    default: null
  }
});

const emit = defineEmits([
  "edited",
  "cursor"
]);

const messagingConditionalTextStore =
  messagingConditionalTextModule();

const html = ref(null);
const activeItems = ref(null);

const editors = reactive({
  basic: null,
  html: null
});

const htmlEditor = computed({
  get() {
    return editors.html;
  },

  set(value) {
    editors.html = value;
  }
});

const basicEditor = computed({
  get() {
    return editors.basic;
  },

  set(value) {
    editors.basic = value;
  }
});

const editor = computed(() => {
  switch (props.editorType) {
    case "html":
      return htmlEditor.value;

    case "basic":
    default:
      return basicEditor.value;
  }
});

const show = computed(() => {
  return (
    props.editorType !== null &&
    htmlEditor.value &&
    basicEditor.value
  );
});

const showToolbar = computed(() => {
  return (
    !props.readOnly &&
    props.editorType === "html" &&
    htmlEditor.value
  );
});

const configure = computed(() => {
  return {
    link: {
      openOnClick: true,
      defaultProtocol: "https",
      protocols: [
        "ftp",
        "mailto",
        "git",
        "cal"
      ],
      HTMLAttributes: {
        target: "_blank"
      }
    },
    mention: {
      suggestions: [],
      deleteTriggerWithBackspace: true
    },
    starterKit: {
      heading: {
        levels: [1, 2, 3]
      },
      link: false,
      underline: false
    },
    youTube: {
      modestBranding: true,
      inline: true,
      nocookie: true
    }
  };
});

const mentionConditionalText = computed(() => {
  return Mention.extend({
    name: "mentionConditionalText",

    addAttributes() {
      return {
        ...this.parent?.(),

        onclick: {
          default: null,
          parseHTML: element => element.getAttribute("onclick"),
          renderHTML: attributes => ({
            onclick: attributes.onclick
          }),
          renderText: attributes => {
            return `onclick="${attributes.onclick}"`;
          }
        }
      };
    },

    parseHTML() {
      return [
        {
          tag: "conditional-text"
        }
      ];
    },

    renderHTML({ HTMLAttributes, node }) {
      return [
        "conditional-text",
        HTMLAttributes,
        `{{ ${node.attrs.label} }}`
      ];
    },

    renderText({ node }) {
      return `<conditional-text data-type="mentionConditionalText" onclick="${node.attrs.onclick}" data-id="${node.attrs.id}" data-label="${node.attrs.label}">{{ ${node.attrs.label} }}</conditional-text>`;
    }
  }).configure(configure.value.mention);
});

const mentionPipedText = computed(() => {
  return Mention.extend({
    name: "mentionPipedText",

    addAttributes() {
      return {
        ...this.parent?.(),

        class: {
          default: null,
          parseHTML: element => element.getAttribute("class"),
          renderHTML: attributes => ({
            class: attributes.class
          }),
          renderText: attributes => {
            return `class="${attributes.class}"`;
          }
        }
      };
    },

    parseHTML() {
      return [
        {
          tag: "piped-text"
        }
      ];
    },

    renderHTML({ HTMLAttributes, node }) {
      return [
        "piped-text",
        HTMLAttributes,
        `{{ ${node.attrs.label} }}`
      ];
    },

    renderText({ node }) {
      return `<piped-text data-type="mentionPipedText" data-id="${node.attrs.id}" data-label="${node.attrs.label}">{{ ${node.attrs.label} }}</piped-text>`;
    }
  }).configure(configure.value.mention);
});

const extensions = computed(() => {
  return {
    basic: [
      Document,
      mentionConditionalText.value,
      mentionPipedText.value,
      Paragraph,
      Text
    ],
    html: [
      StarterKit.configure(configure.value.starterKit),
      Link.configure(configure.value.link),
      mentionConditionalText.value,
      mentionPipedText.value,
      Underline,
      YouTube.configure(configure.value.youTube)
    ]
  };
});

const onUpdate = computed(() => {
  return {
    basic: ({ editor: currentEditor }) => {
      html.value = currentEditor.getText()
        ? currentEditor.getText({
            blockSeparator: "\n\n"
          })
        : "";

      emit("edited", html.value);
    },

    html: ({ editor: currentEditor }) => {
      html.value = currentEditor.getText()
        ? currentEditor.getHTML()
        : "";

      emit("edited", html.value);
    }
  };
});

const onSelectionUpdate = ({ editor: currentEditor }) => {
  const { view } = currentEditor;
  const { selection } = view.state;

  activeItems.value = {};

  if (selection.$head.nodeBefore?.marks.length) {
    activeItems.value.marks =
      selection.$head.nodeBefore.marks.map(
        mark => mark.type.name
      );
  }

  if (selection.$head.node(1)) {
    activeItems.value.nodes = [
      {
        name: selection.$head.node(1).type.name,
        attributes: selection.$head.node(1).attrs
      }
    ];
  }
};

const editorConfiguration = type => {
  let configuration;

  switch (type) {
    case "html":
      configuration = {
        content: html.value,
        extensions: extensions.value.html,
        onUpdate: onUpdate.value.html
      };
      break;

    case "basic":
    default:
      configuration = {
        content:
          htmlEditor.value?.getText({
            blockSeparator: "\n\n"
          }) || "",
        extensions: extensions.value.basic,
        onUpdate: onUpdate.value.basic
      };
      break;
  }

  return {
    content: configuration.content,
    editable: !props.readOnly,
    extensions: configuration.extensions,
    onUpdate: configuration.onUpdate,
    onSelectionUpdate,
    parseOptions: {
      preserveWhitespace: "full"
    },
    onContentError() {
      console.log(
        "Error while parsing editor content. Please check your input."
      );
    },
    onTransaction: ({ editor: currentEditor }) => {
      emit(
        "cursor",
        currentEditor.view.state.selection.anchor
      );
    }
  };
};

const createHtmlEditor = () => {
  htmlEditor.value =
    new Editor(editorConfiguration("html"));
};

const createBasicEditor = () => {
  basicEditor.value =
    new Editor(editorConfiguration("basic"));
};

const destroyEditors = () => {
  if (htmlEditor.value) {
    htmlEditor.value.destroy();
    htmlEditor.value = null;
  }

  if (basicEditor.value) {
    basicEditor.value.destroy();
    basicEditor.value = null;
  }
};

const createEditors = () => {
  createHtmlEditor();
  createBasicEditor();

  switch (props.editorType) {
    case "html":
      html.value = htmlEditor.value.getText()
        ? htmlEditor.value.getHTML()
        : "";
      break;

    case "basic":
    default:
      html.value = basicEditor.value.getText()
        ? basicEditor.value.getText({
            blockSeparator: "\n\n"
          })
        : "";
      break;
  }

  emit("edited", html.value);
  emit("cursor", null);
};

const getActiveEditor = () => {
  switch (props.editorType) {
    case "html":
      return htmlEditor.value;

    case "basic":
    default:
      return basicEditor.value;
  }
};

const insertConditionalText = conditionalText => {
  const activeEditor = getActiveEditor();

  if (!activeEditor || !conditionalText) {
    return;
  }

  const attrs = {
    id: `${conditionalText.id}`,
    label: `conditional text: ${conditionalText.label}`,
    onclick: `window.updateMessageConditionalTextEditId('${conditionalText.id}')`
  };

  if (conditionalText.status === "update") {
    const items = findChildren(
      activeEditor.state.doc,
      node => conditionalText.id === node.attrs.id
    );

    if (items.length) {
      items.forEach(item => {
        activeEditor
          .chain()
          .deleteRange({
            from: item.pos,
            to: item.pos + item.node.nodeSize
          })
          .insertContentAt(
            item.pos,
            {
              type: "mentionConditionalText",
              attrs
            }
          )
          .run();
      });
    }

    return;
  }

  activeEditor
    .chain()
    .focus(
      conditionalText.cursorPosition !== null
        ? conditionalText.cursorPosition
        : "end"
    )
    .insertContent({
      type: "mentionConditionalText",
      attrs
    })
    .run();
};

const insertPipedText = pipedText => {
  const activeEditor = getActiveEditor();

  if (!activeEditor || !pipedText) {
    return;
  }

  const attrs = {
    id: `${pipedText.id}`,
    label: `piped text: ${pipedText.key}`
  };

  activeEditor
    .chain()
    .focus(
      pipedText.cursorPosition !== null
        ? pipedText.cursorPosition
        : "end"
    )
    .insertContent({
      type: "mentionPipedText",
      attrs
    })
    .run();
};

watch(
  () => props.editorType,
  () => {
    destroyEditors();
    createEditors();
  }
);

watch(
  () => props.content,
  newContent => {
    html.value = newContent;
    destroyEditors();
    createEditors();
  }
);

watch(
  () => props.conditionalTextToPlace,
  insertConditionalText
);

watch(
  () => props.pipedTextToPlace,
  insertPipedText
);

onMounted(async () => {
  html.value = props.content;

  createEditors();

  await nextTick();

  addAttributesToObservedElement(
    ".editor",
    "ProseMirror",
    ".tiptap.ProseMirror",
    [
      {
        name: "aria-label",
        value: "message editor content"
      }
    ]
  );

  addAttributesToElement(
    ".tiptap.ProseMirror",
    [
      {
        name: "aria-label",
        value: "message editor content"
      }
    ]
  );

  window.updateMessageConditionalTextEditId = id => {
    messagingConditionalTextStore.setMessageConditionalTextEditId(id);
  };
});

onBeforeUnmount(() => {
  destroyEditors();
});
</script>

<style lang="scss" scoped>
/*.editor:deep(*) {
  min-width: 100%;
}*/

.editor {
  box-shadow: none;
  border-radius: 4px;
  border: 1px solid map.get($grey, "darker");
  background-color: white;
  overflow: hidden;

  :deep(.ProseMirror) {
    margin: 20px 5px !important;

    .is-editor-empty::before {
      color: map.get($grey, "darker");
      font-style: normal;
    }

    &.ProseMirror-focused:focus-visible {
      outline: none;
    }
  }

  :deep(.content) {
    > div {
      transition: all 2s;
      overflow: auto !important;
      padding: 5px;
    }

    & blockquote {
      border-left: 0.25em solid #dfe2e5;
      color: #6a737d;
      padding-left: 1em;
      margin: 20px 0 !important;
    }

    & h1 {
      font-size: 2em !important;
    }

    & conditional-text,
    & piped-text {
      border-radius: 0.4rem;
      color: map.get($blue, "primary");
      padding: 0;

      &:hover {
        cursor: pointer;
        background-color: map.get($blue, "primary");
        color: white;
      }

      &.invalid-piped-text {
        color: map.get($red, "base");
      }
    }
  }
}
</style>
