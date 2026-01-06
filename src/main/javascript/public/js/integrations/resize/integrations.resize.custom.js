import { resizeObserver } from "https://terracotta-js.s3.us-east-2.amazonaws.com/integrations.resize.js";

document.addEventListener("DOMContentLoaded", (event) => {
  // start observing for resize
  resizeObserver.observe(document.documentElement);
});
