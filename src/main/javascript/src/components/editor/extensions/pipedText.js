import Mention from "@tiptap/extension-mention";

export function createPipedTextExtension() {
  return Mention.extend({
    name: "mentionPipedText",

    addAttributes() {
      return {
        ...this.parent?.(),
        class: {
          default: null,
          parseHTML: element => element.getAttribute("class"),
          renderHTML: attributes => ({
            class: attributes.class
          })
        }
      };
    },

    parseHTML() {
      return [{ tag: "piped-text" }];
    },

    renderHTML({ HTMLAttributes, node }) {
      return [
        "piped-text",
        HTMLAttributes,
        `{{ ${node.attrs.label} }}`
      ];
    },

    renderText({ node }) {
      return `<piped-text data-type="mentionPipedText" data-id="${node.attrs.id}" data-label="${node.attrs.label}">{{ ${node.attrs.label} }}</piped-text>`;
    }
  }).configure({
    suggestions: [],
    deleteTriggerWithBackspace: true
  });
}
