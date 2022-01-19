import { History } from "tiptap-vuetify";

// This is a workaround to get access to these extension classes by referencing
// them from an existing extension
const historyAction = new History().availableActions[0];
const ExtensionActionRenderBtn = historyAction.render.constructor;
const VuetifyIcon = historyAction.render.options.icons.mdi.constructor;

export { ExtensionActionRenderBtn, VuetifyIcon };
