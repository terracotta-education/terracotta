export function parseIframeEmbed(embedCode) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(embedCode, "text/html");
  return doc.querySelector("iframe");
}

export function youtubeParser(url) {
  const regExp = /^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?))\??v?=?([^#&?]*).*/;
  const match = url.match(regExp);
  return match && match[7].length === 11 ? match[7] : false;
}

export default {
  parseIframeEmbed,
  youtubeParser,
};
