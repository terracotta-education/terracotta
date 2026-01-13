/**
 * Custom Web Application Script
 *
 * Integration for resize observer to monitor changes to the document size. non-ES6 module version.
 */

document.addEventListener("DOMContentLoaded", (event) => {
  // Calculate the height of the survey content and post the message to Terracotta parent window
  parent.postMessage(
  {
    "subject": "terracotta_iframe_resize",
    "height": Math.max(
      document.body.offsetHeight,
      document.documentElement.offsetHeight
    )
  },
  "*"
  );

  // start observing for resize
  resizeObserver.observe(document.documentElement);
});
