import { isAllowedUri } from "@tiptap/extension-link";

// the link schemes the editor accepts, besides the http(s) Tiptap always allows
export const LINK_PROTOCOLS = [
  "ftp",
  "mailto",
  "git",
  "cal"
];

export const LINK_URL_ERROR = "Enter the full address, starting with https:// (for example, https://www.example.com).";

// "www.example.com" without a scheme becomes a link relative to the Terracotta page it's on, which
// opens something like .../assessment/<id>/www.example.com, so a link must start with a scheme
const SCHEME = /^[a-z][a-z0-9+.-]*:/i;

/**
 * Whether a link address can be saved: it starts with a scheme, and that scheme is one Tiptap
 * allows (which also rules out javascript: and the like).
 */
export const isLinkUrlAllowed = url => {
  const trimmed = (url || "").trim();

  return SCHEME.test(trimmed) && !!isAllowedUri(trimmed, LINK_PROTOCOLS);
};
