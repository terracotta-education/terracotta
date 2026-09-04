import { VueRenderer } from "@tiptap/vue-3";
import tippy from "tippy.js";

import FieldList from "./PlaceholderList.vue";

export default {
  render: () => {
    let component = null;
    let popup = null;

    return {
      onStart: props => {
        component = new VueRenderer(FieldList, {
          props,
          editor: props.editor
        });

        if (!props.clientRect) {
          return;
        }

        popup = tippy(document.body, {
          getReferenceClientRect: props.clientRect,
          appendTo: () => document.body,
          content: component.element,
          showOnCreate: true,
          interactive: true,
          trigger: "manual",
          placement: "bottom-start"
        });
      },

      onUpdate: props => {
        component?.updateProps(props);

        if (!props.clientRect) {
          return;
        }

        popup?.setProps({
          getReferenceClientRect: props.clientRect
        });
      },

      onKeyDown: props => {
        if (props.event.key === "Escape") {
          popup?.hide();
          return true;
        }

        return component?.ref?.onKeyDown(props) || false;
      },

      onExit: () => {
        popup?.destroy();
        component?.destroy();

        popup = null;
        component = null;
      }
    };
  }
};
