<template>
  <div
    v-if="editor"
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
        :editor="editor"
        :activeItems="activeItems"
      />
    </v-card>
  </div>
</template>

<script>
import { Editor, EditorContent } from "@tiptap/vue-2";
import StarterKit from "@tiptap/starter-kit";
import Link from "@tiptap/extension-link";
import Placeholder from '@tiptap/extension-placeholder'
import Underline from "@tiptap/extension-underline";
import YouTube from "@tiptap/extension-youtube";
import ToolBar from "./ToolBar";

export default {
  components: {
    EditorContent,
    ToolBar
  },
  props: {
    html: {
      type: String,
      required: false,
    }
  },
  data() {
    return {
      editor: null,
      activeItems: null,
      rules: [
        (v) => (v && !!v.trim()) || "required",
        (v) => (v || "").length <= 255 || "A maximum of 255 characters is allowed",
      ]
    }
  },
  mounted() {
    this.editor = new Editor({
      content: this.html,
      extensions: [
        StarterKit.configure(
          {
            heading: {
              levels: [1, 2, 3]
            }
          }
        ),
        Link.configure(
          {
            openOnClick: true,
            defaultProtocol: "https",
            protocols: ["ftp", "mailto", "git", "cal"],
            HTMLAttributes: {
              target: "_blank",
            },
          }
        ),
        Placeholder.configure(
          {
            placeholder: "Question"
          }
        ),
        Underline,
        YouTube.configure(
          {
            modestBranding: true,
            inline: true,
            nocookie: true
          }
        )
      ],
      onUpdate: ({ editor }) => {
        this.$emit("edited", editor.getText() ? editor.getHTML() : "");
      },
      onSelectionUpdate: ({ editor }) => {
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
      }
    })
  },
  beforeDestroy() {
    this.editor.destroy();
  }
}
</script>

<style lang="scss" scoped>
.editor::v-deep {
  box-shadow: none;
  border-radius: 4px;
  border: 1px solid map-get($grey, "base");
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
  }
}
</style>
