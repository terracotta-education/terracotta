<template>
  <a
    href="#terracotta-main"
    class="skip-to-content-link"
    tabindex="0"
  >
    Skip to main content
  </a>
</template>

<style lang="scss" scoped>
.skip-to-content-link {
  position: absolute;
  clip: rect(0, 0, 0, 0);
  clip-path: inset(50%);
  width: 1px;
  height: 1px;
  overflow: hidden;
  white-space: nowrap;

  &:focus {
    clip: auto;
    clip-path: none;
    position: static;
    width: auto;
    height: auto;
    background-color: #ffc;
    border: 1px dashed map.get($red, "base");
    padding: 8px;
    z-index: 100;
  }
}
</style>
