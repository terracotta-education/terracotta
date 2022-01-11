import {
  ExtensionActionRenderBtn,
  VuetifyIcon,
} from "./tiptap-vuetify-extensions";

// tiptap-vuetify extension - adds a button to the toolbar
export default class YoutubeEmbedExtension {
  get availableActions() {
    return [
      {
        render: new ExtensionActionRenderBtn({
          nativeExtensionName: "iframe",
          tooltip: "Embed Youtube video",
          icons: {
            mdi: new VuetifyIcon("mdi-youtube"),
          },
          // Button's click handler
          onClick: ({ editor }) => {
            console.log("button clicked");
            editor.commands.iframe({
              youtubeID: "mmrjVNXlrt4",
            });
          },
          // Is the button active? This affects the style of the button.
          // isActive: () => {
          //     return !this.isEditable;
          // },
        }),
      },
    ];
  }
}
