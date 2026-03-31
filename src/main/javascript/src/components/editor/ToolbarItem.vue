<template>
<v-btn-toggle
  v-model="toggled"
  :key="nonActivatableItemKey"
>
  <tool-tip
    :icon="icon"
    :content="title"
    :ref="ref"
    @clicked="handleAction"
    @is-opened="$emit('is-hovered', ref)"
    alignment="top"
    activatorType="button"
    activatorClass="item-button"
    size="small"
  />
</v-btn-toggle>
</template>

<script>
import ToolTip from "@/components/ToolTip.vue";

export default {
  components: {
    ToolTip
  },
  props: {
    editor: {
      type: Object,
      required: true
    },
    icon: {
      type: String,
      required: true
    },
    title: {
      type: String,
      required: true
    },
    action: {
      type: String,
      required: true
    },
    activatable: {
      type: Boolean,
      default: false
    },
    attributes: {
      type: Object,
      required: false
    },
    activate: {
      type: Boolean,
      required: false
    }
  },
  data: () => ({
    buttonIndex: 0,
    toggled: null,
    nonActivatableItemKey: 0
  }),
  watch: {
    editor: {
      handler(editor) {
        if (!this.activatable) {
          this.toggled = null;
          return;
        }

        if (this.attributes) {
          if (editor.isActive(this.action, this.attributes)) {
            return;
          }

          this.toggled = null;
          return;
        }

        if (editor.isActive(this.action)) {
          return;
        }

        this.toggled = null;
      },
      deep: true
    },
    activate: {
      handler(on) {
        if (!this.activatable) {
          this.toggled = null;
          return;
        }

        if (!on && this.editor.isActive(this.action, this.attributes)) {
          // item is still active; don't toggle off
          return;
        }

        if (on && this.editor.isActive(this.action, this.attributes)) {
          this.toggled = this.buttonIndex;
          return;
        }

        this.toggled = null;
      }
    }
  },
  computed: {
    ref() {
      return `tooltip-item-${this.icon}`;
    }
  },
  methods: {
    handleAction() {
      this.$emit("clicked", this.action, this.attributes);
      this.reloadNonActivatableItem();
    },
    reloadNonActivatableItem() {
      if (!this.activatable) {
        // force reload to unset active toggle
        this.nonActivatableItemKey++;
      }
    }
  }
};
</script>

<style scoped>
.v-btn-toggle::v-deep {
  background-color: transparent !important;
  & .item-button {
    margin: 2px 6px;
    border: none !important;
    border-radius: 50% !important;
    &.v-btn--icon:first-child {
      margin-left: 6px !important;
    }
    & .theme--light.v-icon {
      color: rgba(0, 0, 0, 0.7) !important;
    }
  }
}
</style>
