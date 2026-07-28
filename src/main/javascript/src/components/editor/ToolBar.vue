<template>
  <div class="toolbar">
    <v-toolbar
      color="grey-lighten-4"
      height="auto"
      density="compact"
      flat
    >
      <ToolbarItem
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

    <LinkDialog
      v-if="showLinkDialog"
      :editor="editor"
      :href="currentLinkHref"
      @close="showLinkDialog = false"
      @submit="handleLinkDialogSubmit"
    />

    <YouTubeDialog
      v-if="showYouTubeDialog"
      :editor="editor"
      :embed-code="currentYouTubeEmbedCode"
      @close="showYouTubeDialog = false"
      @submit="handleYouTubeDialogSubmit"
    />
  </div>
</template>

<script setup>
import {
  ref,
  watch,
  onMounted
} from "vue";

import LinkDialog from "./components/LinkDialog.vue";
import ToolbarItem from "./ToolbarItem.vue";
import YouTubeDialog from "./components/YouTubeDialog.vue";

defineOptions({
  name: "ToolBar"
});

const props = defineProps({
  editor: {
    type: Object,
    required: true
  },
  activeItems: {
    type: Object,
    default: () => ({})
  }
});

const items = ref([
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
    attributes: {
      level: 1
    },
    activate: false
  },
  {
    icon: "mdi-format-header-2",
    title: "Heading 2",
    action: "heading",
    activatable: true,
    attributes: {
      level: 2
    },
    activate: false
  },
  {
    icon: "mdi-format-header-3",
    title: "Heading 3",
    action: "heading",
    activatable: true,
    attributes: {
      level: 3
    },
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
  }
]);

const itemRefs = ref([]);

const showLinkDialog = ref(false);
const showYouTubeDialog = ref(false);
const currentLinkHref = ref("");
const currentYouTubeEmbedCode = ref("");

watch(
  () => props.activeItems,
  activeItems => {
    items.value.forEach(item => {
      item.activate = false;
    });

    if (
      !activeItems ||
      !Object.keys(activeItems).length
    ) {
      return;
    }

    if (activeItems.marks?.length) {
      activeItems.marks.forEach(mark => {
        const item = items.value.find(
          currentItem =>
            currentItem.action === mark
        );

        if (item) {
          item.activate = true;
        }
      });
    }

    if (activeItems.nodes?.length) {
      activeItems.nodes.forEach(node => {
        const matchingItems = items.value.filter(
          item => item.action === node.name
        );

        if (
          node.attributes &&
          node.attributes.level
        ) {
          const item = matchingItems.find(
            currentItem =>
              currentItem.attributes?.level ===
              node.attributes.level
          );

          if (item) {
            item.activate = true;
          }

          return;
        }

        if (matchingItems[0]) {
          matchingItems[0].activate = true;
        }
      });
    }
  },
  {
    deep: true
  }
);

const clicked = (
  item,
  attrs = {}
) => {
  switch (item) {
    case "undo":
      undo();
      break;

    case "redo":
      redo();
      break;

    case "blockquote":
      blockquote();
      break;

    case "underline":
      underline();
      break;

    case "strike":
      strike();
      break;

    case "italic":
      italic();
      break;

    case "bulletList":
      bulletList();
      break;

    case "orderedList":
      orderedList();
      break;

    case "heading":
      heading(attrs.level);
      break;

    case "code":
      code();
      break;

    case "horizontalRule":
      horizontalRule();
      break;

    case "bold":
      bold();
      break;

    case "paragraph":
      paragraph();
      break;

    case "youtube":
      youTube();
      break;

    case "link":
      link();
      break;

    default:
      break;
  }
};

const undo = () => {
  props.editor
    ?.chain()
    .focus()
    .undo()
    .run();
};

const redo = () => {
  props.editor
    ?.chain()
    .focus()
    .redo()
    .run();
};

const blockquote = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleBlockquote()
    .run();
};

const underline = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleUnderline()
    .run();
};

const strike = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleStrike()
    .run();
};

const italic = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleItalic()
    .run();
};

const bulletList = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleBulletList()
    .run();
};

const orderedList = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleOrderedList()
    .run();
};

const heading = level => {
  for (let i = 1; i <= 3; i++) {
    if (
      i !== level &&
      props.editor?.isActive(
        "heading",
        {
          level: i
        }
      )
    ) {
      props.editor
        .chain()
        .focus()
        .toggleHeading({
          level: i
        })
        .run();
    }
  }

  props.editor
    ?.chain()
    .focus()
    .toggleHeading({
      level
    })
    .run();
};

const bold = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleBold()
    .run();
};

const code = () => {
  props.editor
    ?.chain()
    .focus()
    .toggleCode()
    .run();
};

const horizontalRule = () => {
  props.editor
    ?.chain()
    .focus()
    .setHorizontalRule()
    .run();
};

const paragraph = () => {
  props.editor
    ?.chain()
    .focus()
    .setParagraph()
    .run();
};

const youTube = () => {
  currentYouTubeEmbedCode.value =
    props.editor?.getAttributes("youtube")?.src || "";

  showYouTubeDialog.value = true;
};

const handleYouTubeDialogSubmit = result => {
  showYouTubeDialog.value = false;

  if (!result || result.src === null) {
    return;
  }

  if (result.src === "") {
    props.editor
      ?.chain()
      .focus()
      .extendMarkRange("youtube")
      .clearContent()
      .run();

    return;
  }

  props.editor
    ?.commands
    .setYoutubeVideo(result);
};

const link = () => {
  currentLinkHref.value =
    props.editor?.getAttributes("link")?.href || "";

  showLinkDialog.value = true;
};

const handleLinkDialogSubmit = result => {
  showLinkDialog.value = false;

  if (result === null) {
    return;
  }

  if (result === "") {
    props.editor
      ?.chain()
      .focus()
      .extendMarkRange("link")
      .unsetLink()
      .run();

    return;
  }

  props.editor
    ?.chain()
    .focus()
    .extendMarkRange("link")
    .setLink({
      href: result
    })
    .run();
};

onMounted(() => {
  items.value.forEach(item => {
    itemRefs.value.push(
      `item-${item.icon}`
    );
  });
});
</script>

<style lang="scss" scoped>
.toolbar {
  border-top: 1px solid map.get($grey, "darker");
  border-radius: 0 !important;

  & .v-toolbar {
    display: flex;
    padding: 5px;

    & :deep(.v-toolbar__content) {
      flex-wrap: wrap;
      padding: 0;
    }
  }
}
</style>
