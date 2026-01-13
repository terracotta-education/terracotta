/**
 * Qualtrics Survey Script
 *
 * Integration for resize observer to monitor changes to the document size. ES6 module version.
 */

import { resizeObserver } from "https://terracotta-js.s3.us-east-2.amazonaws.com/integrations.resize.js";

Qualtrics.SurveyEngine.addOnReady(function() {
  // start observing for resize
  resizeObserver.observe(document.documentElement);
});
