export function serializeEditorContent(editor, editorType) {
  if (!editor?.getText()) {
    return "";
  }

  if (editorType === "html") {
    return editor.getHTML();
  }

  return editor.getText({
    blockSeparator: "\n\n"
  });
}
