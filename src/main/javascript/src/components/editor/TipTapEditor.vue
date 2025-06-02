<template>
<div
  v-if="show"
  class="editor mb-6 outlined"
>
  <v-card
    flat
  >
    <editor-content
      :editor="editor"
      class="content"
    />
    <tool-bar
      v-if="showToolbar"
      :editor="editor"
      :activeItems="activeItems"
    />
  </v-card>
</div>
</template>

<script>
import { mapGetters } from "vuex";
import { Editor, EditorContent } from "@tiptap/vue-2";
import { findChildren } from "@tiptap/core";
import store from "@/store/index.js";
import Document from "@tiptap/extension-document";
import Link from "@tiptap/extension-link";
import Mention from "@tiptap/extension-mention";
import Paragraph from "@tiptap/extension-paragraph";
import StarterKit from "@tiptap/starter-kit";
import Text from "@tiptap/extension-text";
import Underline from "@tiptap/extension-underline";
import YouTube from "@tiptap/extension-youtube";
import ToolBar from "./ToolBar";

export default {
  components: {
    EditorContent,
    ToolBar
  },
  props: {
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
      type: Object
    },
    pipedTextToPlace: {
      type: Object
    }
  },
  data: () => ({
    html: null,
    activeItems: null,
    editors: {
      basic: null,
      html: null
    }
  }),
  watch: {
    editorType: {
      handler() {
        this.destroyEditors();
        this.createEditors();
      },
      immediate: false
    },
    content: {
      handler(newContent) {
        this.html = newContent;
        this.destroyEditors();
        this.createEditors();
      }
    },
    conditionalTextToPlace: {
      handler(newConditionalTextToPlace) {
        let editor = null;

        switch(this.editorType) {
          case "html":
            editor = this.htmlEditor;
            break;
          case "basic":
          default:
            editor = this.basicEditor;
            break;
        }

        const attrs = {
          id: `${newConditionalTextToPlace.id}`,
          label: `conditional text: ${newConditionalTextToPlace.label}`,
          onclick: `updateMessageConditionalTextEditId('${newConditionalTextToPlace.id}')`,
        };

        if (newConditionalTextToPlace.status === "update") {
          // delete the existing conditional text)s) and replace with the newly-updated one
          const items = findChildren(editor.state.doc, node => {
            return newConditionalTextToPlace.id === node.attrs.id;
          })

          if (items.length) {
            items.forEach(
              item => {
                editor
                  .chain()
                  .deleteRange({
                    from: item.pos,
                    to: item.pos + item.node.nodeSize
                  })
                  .insertContentAt(
                    item.pos,
                    {
                      type: "mentionConditionalText",
                      attrs: attrs
                    }
                  )
                  .run();
              }
            );
          }

          return;
        }

        editor
          .chain()
          .focus(newConditionalTextToPlace.cursorPosition !== null ? newConditionalTextToPlace.cursorPosition : "end")
          .insertContent(
            {
              type: "mentionConditionalText",
              attrs: attrs
            }
          )
          .run();
      },
      immediate: false
    },
    pipedTextToPlace: {
      handler(newPipedTextToPlace) {
        let editor = null;

        switch(this.editorType) {
          case "html":
            editor = this.htmlEditor;
            break;
          case "basic":
          default:
            editor = this.basicEditor;
            break;
        }

        const attrs = {
          id: `${newPipedTextToPlace.id}`,
          label: `piped text: ${newPipedTextToPlace.key}`
        };

        editor
          .chain()
          .focus(newPipedTextToPlace.cursorPosition !== null ? newPipedTextToPlace.cursorPosition : "end")
          .insertContent(
            {
              type: "mentionPipedText",
              attrs: attrs
            }
          )
          .run();
      },
      immediate: false
    }
  },
  computed: {
    ...mapGetters({
      messageConditionalTextEditId: "messagingConditionalText/messageConditionalTextEditId"
    }),
    editor() {
      switch (this.editorType) {
        case "html":
          return this.htmlEditor;
        case "basic":
        default:
          return this.basicEditor;
      }
    },
    htmlEditor: {
      get() {
        return this.editors.html;
      },
      set(editor) {
        this.editors.html = editor;
      }
    },
    basicEditor: {
      get() {
        return this.editors.basic;
      },
      set(editor) {
        this.editors.basic = editor;
      }
    },
    show() {
      return this.editorType !== null && this.htmlEditor && this.basicEditor;
    },
    showToolbar() {
      return !this.readOnly && this.editorType === "html" && this.htmlEditor;
    },
    mentionConditionalText() {
      return Mention.extend(this.extendeds.conditionalText).configure(this.configure.mention);
    },
    mentionPipedText() {
      return Mention.extend(this.extendeds.pipedText).configure(this.configure.mention);
    },
    extendeds() {
      return {
        conditionalText: {
          name: "mentionConditionalText",
          addAttributes() {
            return {
              ...this.parent?.(),
              onclick: {
                default: null,
                parseHTML: (element) => element.getAttribute("onclick"),
                renderHTML: (attributes) => {
                  return {
                    onclick: attributes.onclick
                  }
                },
                renderText: (attributes) => {
                  return `onclick="${attributes.onclick}"`;
                }
              }
            }
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
            return `<conditional-text data-type='mentionConditionalText' onclick="${node.attrs.onclick}" data-id='${node.attrs.id}' data-label='${node.attrs.label}'>{{ ${node.attrs.label} }}</conditional-text>`;
          }
        },
        pipedText: {
          name: "mentionPipedText",
          addAttributes() {
            return {
              ...this.parent?.(),
              class: {
                default: null,
                parseHTML: (element) => element.getAttribute("class"),
                renderHTML: (attributes) => {
                  return {
                    class: attributes.class
                  }
                },
                renderText: (attributes) => {
                  return `class="${attributes.class}"`;
                }
              }
            }
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
            return `<piped-text data-type='mentionPipedText' data-id='${node.attrs.id}' data-label='${node.attrs.label}'>{{ ${node.attrs.label} }}</piped-text>`;
          }
        }
      };
    },
    configure() {
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
          },
        },
        mention: {
          suggestions: [],
          deleteTriggerWithBackspace: true
        },
        starterKit : {
          heading: {
            levels: [1, 2, 3]
          }
        },
        youTube: {
          modestBranding: true,
          inline: true,
          nocookie: true
        }
      };
    },
    extensions() {
      return {
        basic: [
          Document,
          this.mentionConditionalText,
          this.mentionPipedText,
          Paragraph,
          Text
        ],
        html: [
          StarterKit.configure(this.configure.starterKit),
          Link.configure(this.configure.link),
          this.mentionConditionalText,
          this.mentionPipedText,
          Underline,
          YouTube.configure(this.configure.youTube)
        ]
      };
    },
    onUpdate() {
      return {
        basic: ({ editor }) => {
          this.html = editor.getText() ? editor.getText({ blockSeparator: "\n\n" }) : "";
          this.$emit("edited", this.html);
        },
        html: ({ editor }) => {
          this.html = editor.getText() ? editor.getHTML() : "";
          this.$emit("edited", this.html);
        }
      };
    },
    onSelectionUpdate() {
      return ({ editor }) => {
        const { view } = editor;
        const { selection } = view.state;
        this.activeItems = {};

        if (selection.$head.nodeBefore?.marks.length) {
          this.activeItems.marks = selection.$head.nodeBefore.marks.map(m => m.type.name);
        }

        if (selection.$head.node(1)) {
          this.activeItems.nodes = [
            {
              name: selection.$head.node(1).type.name,
              attributes: selection.$head.node(1).attrs
            }
          ];
        }
      };
    }
  },
  methods: {
    editorConfiguration(type) {
      let configurations = {};
      switch (type) {
        case "html":
          configurations = {
            content: this.html,
            extensions: this.extensions.html,
            onUpdate: this.onUpdate.html
          }
          break;
        case "basic":
        default:
          configurations = {
            content: this.htmlEditor.getText({ blockSeparator: "\n\n" }),
            extensions: this.extensions.basic,
            onUpdate: this.onUpdate.basic
          }
          break;
      }

      return {
        content: configurations.content,
        editable: !this.readOnly,
        extensions: configurations.extensions,
        onUpdate: configurations.onUpdate,
        onSelectionUpdate: this.onSelectionUpdate,
        parseOptions: {
          preserveWhitespace: "full",
        },
        onContentError() {
          console.log("Error while parsing editor content. Please check your input.");
        },
        onTransaction: ({ editor }) => {
          this.$emit("cursor", editor.view.state.selection.anchor);
        }
      };
    },
    createBasicEditor() {
      this.basicEditor = new Editor(this.editorConfiguration("basic"));
    },
    createHtmlEditor() {
      this.htmlEditor = new Editor(this.editorConfiguration("html"));
    },
    createEditors() {
      // creation order is important! 1. html 2. basic
      this.createHtmlEditor();
      this.createBasicEditor();

      switch (this.editorType) {
        case "html":
          this.html = this.htmlEditor.getText() ? this.htmlEditor.getHTML() : "";
          break;
        case "basic":
        default:
          this.html = this.basicEditor.getText() ? this.basicEditor.getText({ blockSeparator: "\n\n" }) : "";
          break;
      }

      this.$emit("edited", this.html);
      this.$emit("cursor", null);
    },
    destroyEditors() {
      if (this.htmlEditor) {
        this.htmlEditor.destroy();
        this.htmlEditor = null;
      }

      if (this.basicEditor) {
        this.basicEditor.destroy();
        this.basicEditor = null;
      }
    }
  },
  mounted() {
    this.html = this.content;
    this.createEditors();
  },
  beforeDestroy() {
    this.destroyEditors();
  }
}

// global function for onclick event to edit conditional text
// eslint-disable-next-line
window.updateMessageConditionalTextEditId = function(id) {
  store.commit("messagingConditionalText/setMessageConditionalTextEditId", id);
}
</script>

<style lang="scss" scoped>
.editor::v-deep {
  min-width: 100%;
  box-shadow: none;
  border-radius: 4px;
  border: 1px solid map-get($grey, "base");
  background-color: white;
  overflow: hidden;
  .ProseMirror {
    margin: 20px 5px !important;
    .is-editor-empty::before {
      color: map-get($grey, "darken-1");
      font-style: normal;
    }
    &.ProseMirror-focused:focus-visible {
      outline: none;
    }
  }
  .content {
    > div {
      transition: all 2s;
      overflow: auto !important;
      padding: 5px;
    }
    & blockquote {
      border-left: .25em solid #dfe2e5;
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
      color: #0077d2;
      padding: 0;
      &:hover {
        cursor: pointer;
        background-color: #0077d2;
        color: white;
      }
      &.invalid-piped-text {
        color: red;
      }
    }
  }
}
</style>
