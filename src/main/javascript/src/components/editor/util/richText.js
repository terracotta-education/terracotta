// what the rich text editor saves always starts with one of its block elements; anything else
// stored where rich text now goes was typed before, as plain text
const RICH_TEXT = /^<(p|ul|ol|blockquote|h[1-6]|pre|hr)[\s/>]/i;

export const isRichText = value => {
  return RICH_TEXT.test((value || "").trim());
};

const escapeHtml = value => {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
};

/**
 * The HTML to load into the rich text editor: rich text as it is, plain text as paragraphs (a blank
 * line starts a new one, a single line break stays a line break), so neither its line breaks nor
 * characters like "<" are lost.
 */
export const toRichText = value => {
  if (!value) {
    return "";
  }

  if (isRichText(value)) {
    return value;
  }

  return value
    .replace(/\r\n?/g, "\n")
    .split(/\n{2,}/)
    .map(paragraph => `<p>${escapeHtml(paragraph).replace(/\n/g, "<br>")}</p>`)
    .join("");
};

// a block ends a word even when the next block's text follows with no space in the HTML
const BLOCK_END = /<\/(p|li|h[1-6]|blockquote|pre)>|<br\s*\/?>/gi;

/**
 * The text of rich text, without its markup (for counting words).
 */
export const richTextToPlainText = value => {
  if (!value) {
    return "";
  }

  if (!isRichText(value)) {
    return value;
  }

  const document = new DOMParser().parseFromString(value.replace(BLOCK_END, "$& "), "text/html");

  return document.body.textContent;
};
