import { describe, it, expect } from "vitest";

import { sanitizeHtml } from "./sanitize-html";

describe("sanitizeHtml", () => {
  it("returns an empty string for empty input", () => {
    expect(sanitizeHtml("")).toBe("");
    expect(sanitizeHtml(null)).toBe("");
    expect(sanitizeHtml(undefined)).toBe("");
  });

  it("keeps the formatting the editor produces", () => {
    const html = "<h2>Title</h2><p><strong>bold</strong> <em>italic</em> <u>underline</u> <s>strike</s></p>" +
      "<ul><li><p>one</p></li></ul><ol><li><p>two</p></li></ol><blockquote><p>quote</p></blockquote>";

    expect(sanitizeHtml(html)).toBe(html);
  });

  it("keeps links that open in a new tab", () => {
    const html = '<p><a target="_blank" rel="noopener noreferrer nofollow" href="https://example.com">link</a></p>';

    expect(sanitizeHtml(html)).toBe(html);
  });

  it("keeps YouTube embeds", () => {
    const html = '<div data-youtube-video=""><iframe width="640" height="480" allowfullscreen="true" ' +
      'src="https://www.youtube-nocookie.com/embed/abc123?modestbranding=1"></iframe></div>';

    expect(sanitizeHtml(html)).toBe(html);
  });

  it("removes script elements", () => {
    expect(sanitizeHtml("<p>hi</p><script>alert(1)</script>")).toBe("<p>hi</p>");
  });

  it("removes event handler attributes", () => {
    expect(sanitizeHtml('<img src="x" onerror="alert(1)"><p onclick="alert(1)">hi</p>'))
      .toBe('<img src="x"><p>hi</p>');
  });

  it("removes javascript: links", () => {
    expect(sanitizeHtml('<a href="javascript:alert(1)">link</a>')).toBe("<a>link</a>");
  });

  it("removes iframes that are not YouTube embeds", () => {
    expect(sanitizeHtml('<p>hi</p><iframe src="https://evil.example.com/embed/"></iframe>')).toBe("<p>hi</p>");
    expect(sanitizeHtml('<iframe src="javascript:alert(1)"></iframe>')).toBe("");
    expect(sanitizeHtml('<iframe srcdoc="<script>alert(1)</script>"></iframe>')).toBe("");
  });

  it("removes srcdoc from an allowed iframe", () => {
    expect(sanitizeHtml('<iframe src="https://www.youtube.com/embed/abc" srcdoc="<script>alert(1)</script>"></iframe>'))
      .toBe('<iframe src="https://www.youtube.com/embed/abc"></iframe>');
  });
});
