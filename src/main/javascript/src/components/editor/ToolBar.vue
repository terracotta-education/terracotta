<template>
  <div
    class="toolbar"
  >
    <v-toolbar
      color="grey lighten-4"
      height="auto"
      dense
      flat
    >
      <toolbar-item
        v-for="(item, index) in items"
        :key="index"
        :editor="editor"
        :icon="item.icon"
        :title="item.title"
        :action="item.action"
        :activatable="item.activatable || false"
        :attributes="item.attributes"
        :activate="item.activate"
        @clicked="clicked"
      />
    </v-toolbar>
  </div>
</template>

<script>
import Vue from "vue";
import LinkDialog from "./components/LinkDialog";
import ToolbarItem from "./ToolbarItem";
import YouTubeDialog from "./components/YouTubeDialog";

export default {
  components: {
    ToolbarItem
  },
  props: {
    editor: {
      type: Object,
      required: true,
    },
    activeItems: {
      type: Object,
      required: false
    }
  },
  data: () => ({
    items: [
      {
        icon: "mdi-undo",
        title: "Undo",
        action: "undo",
        activate: false
      },
      {
        icon: "mdi-redo",
        title: "Redo",
        action: "redo",
        activate: false
      },
      {
        icon: "mdi-format-quote-close",
        title: "Block quote",
        action: "blockquote",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-link",
        title: "Add link",
        action: "link",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-format-underline",
        title: "Underline",
        action: "underline",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-format-strikethrough",
        title: "Strike",
        action: "strike",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-format-italic",
        title: "Italic",
        action: "italic",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-format-list-bulleted",
        title: "Bulleted List",
        action: "bulletList",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-format-list-numbered",
        title: "Ordered List",
        action: "orderedList",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-format-header-1",
        title: "Heading 1",
        action: "heading",
        activatable: true,
        attributes: { level: 1 },
        activate: false
      },
      {
        icon: "mdi-format-header-2",
        title: "Heading 2",
        action: "heading",
        activatable: true,
        attributes: { level: 2 },
        activate: false
      },
      {
        icon: "mdi-format-header-3",
        title: "Heading 3",
        action: "heading",
        activatable: true,
        attributes: { level: 3 },
        activate: false
      },
      {
        icon: "mdi-format-bold",
        title: "Bold",
        action: "bold",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-code-tags",
        title: "Code",
        action: "code",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-minus",
        title: "Horizontal line",
        action: "horizontalRule",
        activatable: false,
        activate: false
      },
      {
        icon: "mdi-format-paragraph",
        title: "Paragraph",
        action: "paragraph",
        activatable: true,
        activate: false
      },
      {
        icon: "mdi-youtube",
        title: "YouTube",
        action: "youtube",
        activatable: true,
        activate: false
      },
    ]
  }),
  watch: {
    activeItems: {
      handler(activeItems) {
        // inactivate all items
        this.items.forEach(item => item.activate = false);

        if (!Object.keys(activeItems).length) {
          // no pre-set toggles
          return;
        }

        //handle marks
        if (activeItems.marks && activeItems.marks.length) {
          activeItems.marks.forEach(
            (mark) => {
              const item = this.items.find(item => item.action === mark);

              if (item) {
                item.activate = true;
              }
            }
          )
        }

        // handle nodes
        if (activeItems.nodes && activeItems.nodes.length) {
          activeItems.nodes.forEach(
            (node) => {
              // find all items with the given action name
              const items = this.items.filter(item => item.action === node.name);

              if (Object.keys(node.attributes).length && node.attributes.level) {
                // a level attribute exists; check item attribite level value
                const item = items.find(item => item.attributes.level === node.attributes.level);

                if (item) {
                  item.activate = true;
                }

                return;
              }

              // no attribuites exist; update first item found
              if (items[0]) {
                items[0].activate = true;
              }
            }
          )
        }
      },
      deep: true
    }
  },
  methods: {
    clicked(item, attrs) {
      switch (item) {
        case "undo":
          this.undo();
          break;
        case "redo":
          this.redo();
          break;
        case "blockquote":
          this.blockquote();
          break;
        case "underline":
          this.underline();
          break;
        case "strike":
          this.strike();
          break;
        case "italic":
          this.italic();
          break;
        case "bulletList":
          this.bulletList();
          break;
        case "orderedList":
          this.orderedList();
          break;
        case "heading":
          this.heading(attrs.level);
          break;
        case "code":
          this.code();
          break;
        case "horizontalRule":
          this.horizontalRule();
          break;
        case "bold":
          this.bold();
          break;
        case "paragraph":
          this.paragraph();
          break;
        case "youtube":
          this.youTube();
          break;
        case "link":
          this.link();
          break;
        default:
          break;
      }
    },
    undo() {
      this.editor
        .chain()
        .focus()
        .undo()
        .run();
    },
    redo() {
      this.editor
        .chain()
        .focus()
        .redo()
        .run();
    },
    blockquote() {
      this.editor
        .chain()
        .focus()
        .toggleBlockquote()
        .run();
    },
    underline() {
      this.editor
        .chain()
        .focus()
        .toggleUnderline()
        .run();
    },
    strike() {
      this.editor
        .chain()
        .focus()
        .toggleStrike()
        .run();
    },
    italic() {
      this.editor
        .chain()
        .focus()
        .toggleItalic()
        .run();
    },
    bulletList() {
      this.editor
        .chain()
        .focus()
        .toggleBulletList()
        .run();
    },
    orderedList() {
      this.editor
        .chain()
        .focus()
        .toggleOrderedList()
        .run();
    },
    heading(level) {
      // toggle all previously-active headers off
      for (let i = 1; i <= 3; i++) {
        if (this.editor.isActive("heading", { level: i })) {
          this.editor
            .chain()
            .focus()
            .toggleHeading({ level: i })
            .run();
        }
      }

      // toggle only this header on
      this.editor
        .chain()
        .focus()
        .toggleHeading({ level: level })
        .run();
    },
    bold() {
      this.editor
        .chain()
        .focus()
        .toggleBold()
        .run();
    },
    code() {
      this.editor
        .chain()
        .focus()
        .toggleCode()
        .run();
    },
    horizontalRule() {
      this.editor
        .chain()
        .focus()
        .setHorizontalRule()
        .run();
    },
    paragraph() {
      this.editor
        .chain()
        .focus()
        .setParagraph()
        .run();
    },
    async youTube() {
      const embedCode = this.editor.getAttributes("youtube").src;
      const editor = this.editor;
      const parent = this.$parent;
      var YouTubeDialogClass = Vue.extend(YouTubeDialog);
      const youTubeDialog = new YouTubeDialogClass(
        {
          parent: parent,
          propsData: {
            editor,
            embedCode
          }
        }
      );

      youTubeDialog.$mount();
      document.querySelector("body").appendChild(youTubeDialog.$el);

      // result = {src, height, width}
      const result = await youTubeDialog.openDialog();

      if (result.src === null) {
        return;
      }

      if (result.src === "") {
        this.editor
          .chain()
          .focus()
          .extendMarkRange("youtube")
          .clearContent()
          .run();
        return;
      }

      this.editor
        .commands
        .setYoutubeVideo(result);
    },
    async link() {
      const href = this.editor.getAttributes("link").href;
      const editor = this.editor;
      const parent = this.$parent;
      var LinkDialogClass = Vue.extend(LinkDialog);
      const linkDialog = new LinkDialogClass(
        {
          parent: parent,
          propsData: {
            editor,
            href
          }
        }
      );

      linkDialog.$mount();
      document.querySelector("body").appendChild(linkDialog.$el);

      const result = await linkDialog.openDialog();

      // cancelled
      if (result === null) {
        return;
      }

      // empty
      if (result === "") {
        this.editor
          .chain()
          .focus()
          .extendMarkRange("link")
          .unsetLink()
          .run();

        return;
      }

      // update link
      this.editor
        .chain()
        .focus()
        .extendMarkRange("link")
        .setLink(
          {
            href: result
          }
        )
        .run();
    }
  }
};
</script>

<style scoped>
.toolbar {
  border-top: 1px solid #9e9e9e;
  border-radius: 0 !important;
  & .v-toolbar {
    display: flex;
    padding: 5px;
    & .v-toolbar__content {
      flex-wrap: wrap;
      padding: 0;
    }
  }
}
</style>
