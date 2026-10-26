import { afterEach, describe, expect, it } from "vitest";
import { Editor } from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";
import Link from "@tiptap/extension-link";

import { LINK_PROTOCOLS, isLinkUrlAllowed } from "./link-url";

describe("isLinkUrlAllowed", () => {
  it.each([
    "https://www.google.com",
    "http://example.com/page?x=1",
    "  https://example.com  ",
    "mailto:someone@example.com",
    "ftp://files.example.com"
  ])("allows %s", url => {
    expect(isLinkUrlAllowed(url)).toBe(true);
  });

  // a scheme-less address became a link relative to the page, e.g.
  // .../assessment/<id>/www.google.com
  it.each([
    "www.google.com",
    "google.com",
    "/experiment/1",
    "./page",
    "javascript:alert(1)",
    "",
    null
  ])("refuses %s", url => {
    expect(isLinkUrlAllowed(url)).toBe(false);
  });
});

// the editor configured the way TipTapEditor.vue configures its Link extension
describe("the editor's link rule", () => {
  let editor;

  afterEach(() => {
    editor?.destroy();
  });

  const createEditor = content => {
    editor = new Editor({
      content,
      extensions: [
        StarterKit.configure({ link: false }),
        Link.configure({
          defaultProtocol: "https",
          protocols: LINK_PROTOCOLS,
          isAllowedUri: url => isLinkUrlAllowed(url)
        })
      ]
    });

    return editor;
  };

  it("refuses to set a scheme-less link", () => {
    createEditor("<p>text</p>");
    editor.commands.selectAll();

    expect(editor.commands.setLink({ href: "www.google.com" })).toBe(false);
    expect(editor.getHTML()).not.toContain("<a");
  });

  it("sets a link with a scheme", () => {
    createEditor("<p>text</p>");
    editor.commands.selectAll();

    expect(editor.commands.setLink({ href: "https://www.google.com" })).toBe(true);
    expect(editor.getHTML()).toContain('href="https://www.google.com"');
  });

  it("drops scheme-less links from pasted or loaded content, keeping the text", () => {
    createEditor('<p><a href="www.google.com">Google</a> and <a href="https://example.com">Example</a></p>');

    const html = editor.getHTML();

    expect(html).not.toContain("www.google.com\"");
    expect(html).toContain("Google");
    expect(html).toContain('href="https://example.com"');
  });
});
