import { describe, expect, it } from "vitest";

import { isRichText, richTextToPlainText, toRichText } from "./richText";

describe("isRichText", () => {
  it.each([
    "<p>text</p>",
    "<ul><li><p>item</p></li></ul>",
    "  <ol><li><p>item</p></li></ol>",
    "<h2>heading</h2>"
  ])("treats %s as rich text", value => {
    expect(isRichText(value)).toBe(true);
  });

  it.each([
    "plain text",
    "<3 this",
    "a < b",
    "",
    null
  ])("treats %s as plain text", value => {
    expect(isRichText(value)).toBe(false);
  });
});

describe("toRichText", () => {
  it("keeps rich text as it is", () => {
    expect(toRichText("<p>a <strong>b</strong></p>")).toBe("<p>a <strong>b</strong></p>");
  });

  it("turns plain text into paragraphs and line breaks, escaping markup characters", () => {
    expect(toRichText("one\r\ntwo\n\n<b>three</b> & four")).toBe(
      "<p>one<br>two</p><p>&lt;b&gt;three&lt;/b&gt; &amp; four</p>"
    );
  });

  it("gives empty for nothing", () => {
    expect(toRichText(null)).toBe("");
    expect(toRichText("")).toBe("");
  });
});

describe("richTextToPlainText", () => {
  it("separates the text of adjacent blocks", () => {
    expect(richTextToPlainText("<p>one</p><ul><li><p>two</p></li></ul>").split(/\s+/).filter(Boolean)).toEqual(["one", "two"]);
  });

  it("decodes entities", () => {
    expect(richTextToPlainText("<p>a &amp; b</p>").trim()).toBe("a & b");
  });

  it("leaves plain text alone", () => {
    expect(richTextToPlainText("a < b")).toBe("a < b");
  });
});
