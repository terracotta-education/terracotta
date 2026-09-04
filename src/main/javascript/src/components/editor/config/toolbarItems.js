export const toolbarItems = [
  { icon: "mdi-undo", title: "Undo", action: "undo" },
  { icon: "mdi-redo", title: "Redo", action: "redo" },
  { icon: "mdi-format-quote-close", title: "Block quote", action: "blockquote", activatable: true },
  { icon: "mdi-link", title: "Add link", action: "link", activatable: true },
  { icon: "mdi-format-underline", title: "Underline", action: "underline", activatable: true },
  { icon: "mdi-format-strikethrough", title: "Strike", action: "strike", activatable: true },
  { icon: "mdi-format-italic", title: "Italic", action: "italic", activatable: true },
  { icon: "mdi-format-list-bulleted", title: "Bulleted List", action: "bulletList", activatable: true },
  { icon: "mdi-format-list-numbered", title: "Ordered List", action: "orderedList", activatable: true },
  { icon: "mdi-format-header-1", title: "Heading 1", action: "heading", activatable: true, attributes: { level: 1 } },
  { icon: "mdi-format-header-2", title: "Heading 2", action: "heading", activatable: true, attributes: { level: 2 } },
  { icon: "mdi-format-header-3", title: "Heading 3", action: "heading", activatable: true, attributes: { level: 3 } },
  { icon: "mdi-format-bold", title: "Bold", action: "bold", activatable: true },
  { icon: "mdi-code-tags", title: "Code", action: "code", activatable: true },
  { icon: "mdi-minus", title: "Horizontal line", action: "horizontalRule" },
  { icon: "mdi-format-paragraph", title: "Paragraph", action: "paragraph", activatable: true },
  { icon: "mdi-youtube", title: "YouTube", action: "youtube", activatable: true }
];
