import { resizeObserver } from "https://terracotta-js.s3.us-east-2.amazonaws.com/integrations.resize.js";

Qualtrics.SurveyEngine.addOnReady(function() {
  // start observing for resize
  resizeObserver.observe(document.documentElement);
});
