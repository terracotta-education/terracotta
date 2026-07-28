export function parseIframeEmbed(embedCode) {
  if (!embedCode) {
    return null;
  }

  const parser = new DOMParser();
  const doc = parser.parseFromString(embedCode, "text/html");

  return doc.querySelector("iframe");
}

export function youtubeParser(url) {
  if (!url) {
    return false;
  }

  const match = url.match(/\/embed\/([^?&]+)/);

  return match?.[1] || false;
}
