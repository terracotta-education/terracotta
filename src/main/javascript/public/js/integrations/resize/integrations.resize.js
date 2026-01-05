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
