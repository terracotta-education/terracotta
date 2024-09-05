export function parseIframeEmbed(embedCode) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(embedCode, "text/html");

  return doc.querySelector("iframe");
}

export function youtubeParser(url) {
  const split = url.split("/embed/");

  return split && split.length > 1 ? split[1] : false;
}

export default {
  parseIframeEmbed,
  youtubeParser,
};
