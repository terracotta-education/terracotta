/**
 * Qualtrics Survey Script
 *
 * Integration for resize observer to monitor changes to the document size. non-ES6 module version.
 */

Qualtrics.SurveyEngine.addOnReady(function() {
  const resizeObserver = new ResizeObserver((target) => {
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

  // start observing for resize
  resizeObserver.observe(document.documentElement);
});
