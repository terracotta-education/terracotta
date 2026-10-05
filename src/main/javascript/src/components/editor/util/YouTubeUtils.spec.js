import { describe, it, expect } from "vitest";

import { parseIframeEmbed, youtubeParser } from "./YouTubeUtils";

describe("parseIframeEmbed", () => {
  it("returns null when embedCode is falsy", () => {
    expect(parseIframeEmbed("")).toBe(null);
    expect(parseIframeEmbed(null)).toBe(null);
    expect(parseIframeEmbed(undefined)).toBe(null);
  });

  it("returns the parsed iframe element for valid embed HTML", () => {
    const embedCode =
      '<iframe width="560" height="315" src="https://www.youtube.com/embed/dQw4w9WgXcQ" title="YouTube video player"></iframe>';

    const iframe = parseIframeEmbed(embedCode);

    expect(iframe).not.toBe(null);
    expect(iframe.tagName).toBe("IFRAME");
    expect(iframe.getAttribute("src")).toBe(
      "https://www.youtube.com/embed/dQw4w9WgXcQ"
    );
  });

  it("returns null when the embed HTML contains no iframe element", () => {
    const iframe = parseIframeEmbed("<div>not an iframe</div>");

    expect(iframe).toBe(null);
  });
});

describe("youtubeParser", () => {
  it("returns false when url is falsy", () => {
    expect(youtubeParser("")).toBe(false);
    expect(youtubeParser(null)).toBe(false);
    expect(youtubeParser(undefined)).toBe(false);
  });

  it("extracts the video id from an embed URL", () => {
    expect(
      youtubeParser("https://www.youtube.com/embed/dQw4w9WgXcQ")
    ).toBe("dQw4w9WgXcQ");
  });

  it("extracts the video id when the embed URL has query params", () => {
    expect(
      youtubeParser(
        "https://www.youtube.com/embed/dQw4w9WgXcQ?rel=0&autoplay=1"
      )
    ).toBe("dQw4w9WgXcQ");
  });

  it("stops at the query separator but includes a trailing fragment (matches [^?&]+)", () => {
    // The parsing regex only excludes `?` and `&` from the captured id, so a
    // `#fragment` with no query string is included verbatim in the result.
    expect(
      youtubeParser("https://www.youtube.com/embed/dQw4w9WgXcQ#t=30s")
    ).toBe("dQw4w9WgXcQ#t=30s");
  });

  it("returns false for a non-embed YouTube URL", () => {
    expect(
      youtubeParser("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    ).toBe(false);
  });

  it("returns false for a URL with no /embed/ segment at all", () => {
    expect(youtubeParser("https://example.com/some/path")).toBe(false);
  });
});
