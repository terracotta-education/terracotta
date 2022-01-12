import Vue from "vue";
import {
  ExtensionActionRenderBtn,
  VuetifyIcon,
} from "./tiptap-vuetify-extensions";
import YoutubeEmbedDialog from "./YoutubeEmbedDialog.vue";

// tiptap-vuetify extension - adds a button to the toolbar
export default class YoutubeEmbedExtension {
  get availableActions() {
    const nativeExtensionName = "iframe";
    return [
      {
        render: new ExtensionActionRenderBtn({
          nativeExtensionName,
          tooltip: "Embed Youtube video",
          icons: {
            mdi: new VuetifyIcon("mdi-youtube"),
          },
          // Button's click handler
          onClick: ({ context, editor }) => {
            const YoutubeEmbedDialogComponent = Vue.extend(YoutubeEmbedDialog);
            const instance = new YoutubeEmbedDialogComponent({
              vuetify: Vue.prototype.tiptapVuetifyPlugin.vuetify,
              propsData: {
                nativeExtensionName,
                context,
                editor,
              },
            });

            instance.$mount();
            document.body.appendChild(instance.$el);
          },
          isActive: () => {
            // TODO: implement this? But what would it mean if it were active? It's not a toggle.
            return false;
          },
        }),
      },
    ];
  }
}
