import createDOMPurify from "dompurify";

// The editor embeds YouTube videos from youtube-nocookie.com. Older content may use youtube.com.
const ALLOWED_IFRAME_SRC = /^https:\/\/www\.youtube(-nocookie)?\.com\/embed\//i;

const purifier = createDOMPurify(window);

purifier.addHook("uponSanitizeElement", (node, data) => {
  if (data.tagName === "iframe" && !ALLOWED_IFRAME_SRC.test(node.getAttribute("src") || "")) {
    node.remove();
  }
});

// for plain text, such as a URL parameter, that is put into an HTML string
export function escapeHtml(text) {
  return String(text ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function sanitizeHtml(html) {
  if (!html) {
    return "";
  }

  return purifier.sanitize(html, {
    ADD_TAGS: ["iframe"],
    ADD_ATTR: ["target", "allow", "allowfullscreen", "frameborder"]
  });
}
