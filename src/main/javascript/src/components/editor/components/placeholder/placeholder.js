import { VueRenderer } from "@tiptap/vue-2";
import tippy from "tippy.js"
import FieldList from "./PlaceholderList.vue"

export default {
  render: () => {
    let component;
    let popup;

    return {
      onStart: props => {
        component = new VueRenderer(
          FieldList,
          {
            parent: this,
            propsData: props
          }
        );

        if (!props.clientRect) {
          return;
        }

        popup = tippy(
          "body",
          {
            getReferenceClientRect: props.clientRect,
            appendTo: () => document.body,
            content: component.element,
            showOnCreate: true,
            interactive: true,
            trigger: "manual",
            placement: "bottom-start",
          }
        );
      },
      onUpdate(props) {
        component.updateProps(props);

        if (!props.clientRect) {
          return;
        }

        popup[0].setProps(
          {
            getReferenceClientRect: props.clientRect,
          }
        );
      },
      onKeyDown(props) {
        if (props.event.key === "Escape") {
          popup[0].hide();

          return true;
        }

        return component.ref?.onKeyDown(props);
      },
      onExit() {
        popup[0].destroy();
        component.destroy();
      }
    }
  }
}
