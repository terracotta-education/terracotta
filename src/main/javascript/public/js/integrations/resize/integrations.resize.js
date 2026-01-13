/**
 * Utility resize observer method to monitor changes to the document size. For use with ES6 module code.
 */

export const resizeObserver = new ResizeObserver((target) => {
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
});
