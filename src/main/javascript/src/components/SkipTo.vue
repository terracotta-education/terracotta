<template>
<a
href="#terracotta-main"
class="skip-to-content-link"
tabindex="0"
>
Skip to main content
</a>
</template>

<style lang="scss" >
@import "@/styles/variables";

.skip-to-content-link {
  position: absolute;
  left: -10000px;
  top: auto;
  width: 1px;
  height: 1px;
  overflow: hidden;
  &:focus {
    position: static;
    width: auto;
    height: auto;
    background-color: #ffc;
    border: 1px dashed map-get($red, "base");
    padding: 8px;
    z-index: 100;
  }
}
</style>
